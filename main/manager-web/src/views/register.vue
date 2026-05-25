<template>
  <div class="welcome" @keyup.enter="register">
    <el-container style="height: 100%;">
      <el-header>
        <div style="display: flex;align-items: center;margin-top: 15px;margin-left: 10px;gap: 10px;">
          <BrandLogo size="md" />
        </div>
      </el-header>
      <div class="login-person">
        <img loading="lazy" alt="" src="@/assets/login/register-person.png" style="width: 100%;" />
      </div>
      <el-main style="position: relative;">
        <div class="login-box">
          <div style="display: flex;align-items: center;gap: 20px;margin-bottom: 39px;padding: 0 30px;">
            <img loading="lazy" alt="" src="@/assets/login/hi.png" style="width: 34px;height: 34px;" />
            <div class="login-text">{{ $t('register.title') }}</div>
            <div class="login-welcome">
              {{ $t('register.welcome') }}
            </div>
          </div>

          <div style="padding: 0 30px;">
            <form @submit.prevent="register">
              <div class="input-box" v-if="!enableMobileRegister">
                <img loading="lazy" alt="" class="input-icon" src="@/assets/login/username.png" />
                <el-input v-model="form.username" :placeholder="$t('register.usernamePlaceholder')" />
              </div>

              <template v-if="enableMobileRegister">
                <div class="input-box">
                  <div style="display: flex; align-items: center; width: 100%;">
                    <el-select v-model="form.areaCode" style="width: 220px; margin-right: 10px;">
                      <el-option v-for="item in mobileAreaList" :key="item.key" :label="`${item.name} (${item.key})`"
                        :value="item.key" />
                    </el-select>
                    <el-input v-model="form.mobile" :placeholder="$t('register.mobilePlaceholder')" />
                  </div>
                </div>

                <div style="display: flex; align-items: center; margin-top: 20px; width: 100%; gap: 10px;">
                  <div class="input-box" style="width: calc(100% - 130px); margin-top: 0;">
                    <img loading="lazy" alt="" class="input-icon" src="@/assets/login/phone.png" />
                    <el-input v-model="form.mobileCaptcha" :placeholder="$t('register.mobileCaptchaPlaceholder')"
                      style="flex: 1;" maxlength="6" />
                  </div>
                  <el-button type="primary" class="send-captcha-btn" :disabled="!canSendMobileCaptcha"
                    @click="sendMobileCaptcha">
                    <span>
                      {{ countdown > 0 ? `${countdown}${$t('register.secondsLater')}` : $t('register.sendCaptcha') }}
                    </span>
                  </el-button>
                </div>
              </template>

              <div class="input-box">
                <img loading="lazy" alt="" class="input-icon" src="@/assets/login/password.png" />
                <el-input v-model="form.password" :placeholder="$t('register.passwordPlaceholder')" type="password"
                  show-password />
              </div>

              <div class="input-box">
                <img loading="lazy" alt="" class="input-icon" src="@/assets/login/password.png" />
                <el-input v-model="form.confirmPassword" :placeholder="$t('register.confirmPasswordPlaceholder')"
                  type="password" show-password />
              </div>

              <div class="val-auth-back-link" @click="goToLogin">{{ $t('register.goToLogin') }}</div>
            </form>
          </div>

          <div class="login-btn" @click="register">{{ $t('register.registerButton') }}</div>

          <!-- Google OAuth Sign In -->
          <div v-if="googleClientId" class="val-oauth-divider">
            <span>{{ $t('login.or') }}</span>
          </div>
          <div v-if="googleClientId" class="val-oauth-container">
            <div id="google-signin-btn" class="google-btn-wrapper"></div>
          </div>

          <div style="font-size: 14px;color: #979db1;">
          </div>
        </div>
      </el-main>

      <el-footer>
        <version-footer />
      </el-footer>
    </el-container>
  </div>
</template>

<script>
import Api from '@/apis/api';
import BrandLogo from '@/components/BrandLogo.vue';
import VersionFooter from '@/components/VersionFooter.vue';
import { goToPage, showDanger, showSuccess, sm2Encrypt, validateMobile } from '@/utils';
import { mapState } from 'vuex';
import i18n from '@/i18n';


export default {
  name: 'register',
  components: {
    VersionFooter,
    BrandLogo,
  },
  computed: {
    ...mapState({
      allowUserRegister: state => state.pubConfig.allowUserRegister,
      enableMobileRegister: state => state.pubConfig.enableMobileRegister,
      mobileAreaList: state => state.pubConfig.mobileAreaList,
      sm2PublicKey: state => state.pubConfig.sm2PublicKey,
      googleClientId: state => state.pubConfig.googleClientId,
    }),
    currentLanguage() {
      return i18n.locale || "vi";
    },
    xiaozhiAiIcon() {
      return this.currentLanguage === "en"
        ? require("@/assets/xiaozhi-ai_en.png")
        : require("@/assets/xiaozhi-ai_vi.png");
    },
    canSendMobileCaptcha() {
      return this.countdown === 0 && validateMobile(this.form.mobile, this.form.areaCode);
    }
  },
  data() {
    return {
      form: {
        username: '',
        password: '',
        confirmPassword: '',
        areaCode: '+86',
        mobile: '',
        mobileCaptcha: ''
      },
      countdown: 0,
      timer: null,
    }
  },
  mounted() {
    this.$store.dispatch('fetchPubConfig').then(() => {
      if (!this.allowUserRegister) {
        showDanger(this.$t('register.notAllowRegister'));
        setTimeout(() => {
          goToPage('/login');
        }, 1500);
      } else if (this.googleClientId) {
        this.loadGoogleSdk(() => {
          this.initGoogleSignIn();
        });
      }
    });
  },
  methods: {
    // 封装输入验证逻辑
    validateInput(input, message) {
      if (!input.trim()) {
        showDanger(message);
        return false;
      }
      return true;
    },

    // 发送手机验证码
    sendMobileCaptcha() {
      if (!validateMobile(this.form.mobile, this.form.areaCode)) {
        showDanger(this.$t('register.inputCorrectMobile'));
        return;
      }

      // 清除可能存在的旧定时器
      if (this.timer) {
        clearInterval(this.timer);
        this.timer = null;
      }

      // 开始倒计时
      this.countdown = 60;
      this.timer = setInterval(() => {
        if (this.countdown > 0) {
          this.countdown--;
        } else {
          clearInterval(this.timer);
          this.timer = null;
        }
      }, 1000);

      // 调用发送验证码接口
      Api.user.sendSmsVerification({
        phone: this.form.areaCode + this.form.mobile,
        captcha: '',
        captchaId: '',
      }, (res) => {
        showSuccess(this.$t('register.captchaSendSuccess'));
      }, (err) => {
        showDanger(err.data.msg || this.$t('register.captchaSendFailed'));
        this.countdown = 0;
      });
    },

    // 注册逻辑
    async register() {
      if (this.enableMobileRegister) {
        // 手机号注册验证
        if (!validateMobile(this.form.mobile, this.form.areaCode)) {
          showDanger(this.$t('register.inputCorrectMobile'));
          return;
        }
        if (!this.form.mobileCaptcha) {
          showDanger(this.$t('register.requiredMobileCaptcha'));
          return;
        }
      } else {
        // 用户名注册验证
        if (!this.validateInput(this.form.username, this.$t('register.requiredUsername'))) {
          return;
        }
      }

      // 验证密码
      if (!this.validateInput(this.form.password, this.$t('register.requiredPassword'))) {
        return;
      }
      if (this.form.password !== this.form.confirmPassword) {
        showDanger(this.$t('register.passwordsNotMatch'))
        return
      }
      let encryptedPassword;
      try {
        encryptedPassword = sm2Encrypt(this.sm2PublicKey, this.form.password);
      } catch (error) {
        console.error("密码加密失败:", error);
        showDanger(this.$t('sm2.encryptionFailed'));
        return;
      }

      let plainUsername;
      if (this.enableMobileRegister) {
        plainUsername = this.form.areaCode + this.form.mobile;
      } else {
        plainUsername = this.form.username;
      }

      // 准备注册数据
      const registerData = {
        username: plainUsername,
        password: encryptedPassword,
        captchaId: '',
        mobileCaptcha: this.form.mobileCaptcha
      };

      Api.user.register(registerData, ({ data }) => {
        showSuccess(this.$t('register.registerSuccess'))
        goToPage('/login')
      }, (err) => {
        showDanger(err.data.msg || this.$t('register.registerFailed'))
      })
    },

    goToLogin() {
      goToPage('/login')
    },
    getUserInfo() {
      Api.user.getUserInfo(({ data }) => {
        if (data.code === 0) {
          this.$store.commit('setUserInfo', data.data);
          goToPage('/home');
        } else {
          showDanger(this.$t('login.userInfoFailed'));
        }
      });
    },
    loadGoogleSdk(callback) {
      if (window.google && window.google.accounts) {
        callback();
        return;
      }
      const script = document.createElement('script');
      script.src = 'https://accounts.google.com/gsi/client';
      script.async = true;
      script.defer = true;
      script.onload = () => {
        callback();
      };
      document.head.appendChild(script);
    },
    initGoogleSignIn() {
      if (!window.google || !window.google.accounts) return;
      window.google.accounts.id.initialize({
        client_id: this.googleClientId,
        callback: this.handleGoogleCredentialResponse,
      });
      const btnContainer = document.getElementById('google-signin-btn');
      if (btnContainer) {
        window.google.accounts.id.renderButton(
          btnContainer,
          { theme: 'outline', size: 'large', width: btnContainer.offsetWidth || 376 }
        );
      }
    },
    handleGoogleCredentialResponse(response) {
      if (!response.credential) return;
      Api.user.googleLogin(
        response.credential,
        ({ data }) => {
          showSuccess(this.$t('login.loginSuccess'));
          this.$store.commit('setToken', JSON.stringify(data.data));
          this.getUserInfo();
        },
        (err) => {
          showDanger(err.data?.msg || this.$t('login.loginFailed'));
        }
      );
    },
  },
  beforeDestroy() {
    if (this.timer) {
      clearInterval(this.timer);
    }
  }
}
</script>

<style lang="scss" scoped>
@import './auth.scss';

.send-captcha-btn {
  margin-right: -5px;
  min-width: 100px;
  height: 40px;
  line-height: 40px;
  border-radius: 4px;
  font-size: 14px;
  background: rgb(87, 120, 255);
  border: none;
  padding: 0px;

  &:disabled {
    background: #c0c4cc;
    cursor: not-allowed;
  }
}
</style>
