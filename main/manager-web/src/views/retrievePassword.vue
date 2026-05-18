<template>
  <div class="welcome" @keyup.enter="retrievePassword">
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
        <form @submit.prevent="retrievePassword">
          <div class="login-box">
            <div style="display: flex;align-items: center;gap: 20px;margin-bottom: 39px;padding: 0 30px;">
              <img loading="lazy" alt="" src="@/assets/login/hi.png" style="width: 34px;height: 34px;" />
              <div class="login-text">{{ $t('retrievePassword.title') }}</div>
              <div class="login-welcome">
                {{ $t('retrievePassword.subtitle') }}
              </div>
            </div>

            <div style="padding: 0 30px;">
              <div class="input-box">
                <div style="display: flex; align-items: center; width: 100%;">
                  <el-select v-model="form.areaCode" style="width: 220px; margin-right: 10px;">
                    <el-option v-for="item in mobileAreaList" :key="item.key" :label="`${item.name} (${item.key})`"
                      :value="item.key" />
                  </el-select>
                  <el-input v-model="form.mobile" :placeholder="$t('retrievePassword.mobilePlaceholder')" />
                </div>
              </div>

              <div style="display: flex; align-items: center; margin-top: 20px; width: 100%; gap: 10px;">
                <div class="input-box" style="width: calc(100% - 130px); margin-top: 0;">
                  <img loading="lazy" alt="" class="input-icon" src="@/assets/login/phone.png" />
                  <el-input v-model="form.mobileCaptcha" :placeholder="$t('retrievePassword.mobileCaptchaPlaceholder')" style="flex: 1;" maxlength="6" />
                </div>
                <el-button type="primary" class="send-captcha-btn" :disabled="!canSendMobileCaptcha"
                  @click="sendMobileCaptcha">
                  <span>
                    {{ countdown > 0 ? `${countdown}${$t('register.secondsLater')}` : $t('retrievePassword.getMobileCaptcha') }}
                  </span>
                </el-button>
              </div>

              <div class="input-box">
                <img loading="lazy" alt="" class="input-icon" src="@/assets/login/password.png" />
                <el-input v-model="form.newPassword" :placeholder="$t('retrievePassword.newPasswordPlaceholder')" type="password" show-password />
              </div>

              <div class="input-box">
                <img loading="lazy" alt="" class="input-icon" src="@/assets/login/password.png" />
                <el-input v-model="form.confirmPassword" :placeholder="$t('retrievePassword.confirmNewPasswordPlaceholder')" type="password" show-password />
              </div>

              <div style="font-weight: 400;font-size: 14px;text-align: left;color: #5778ff;margin-top: 20px;">
                <div style="cursor: pointer;" @click="goToLogin">{{ $t('retrievePassword.goToLogin') }}</div>
              </div>
            </div>

            <div class="login-btn" @click="retrievePassword">{{ $t('retrievePassword.resetButton') }}</div>
          </div>
        </form>
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
import { goToPage, showDanger, showSuccess, validateMobile, sm2Encrypt } from '@/utils';
import { mapState } from 'vuex';
import i18n from '@/i18n';

export default {
  name: 'retrieve',
  components: {
    VersionFooter,
    BrandLogo,
  },
  computed: {
    ...mapState({
      mobileAreaList: state => state.pubConfig.mobileAreaList,
      sm2PublicKey: state => state.pubConfig.sm2PublicKey
    }),
    canSendMobileCaptcha() {
      return this.countdown === 0 && validateMobile(this.form.mobile, this.form.areaCode);
    }
  },
  data() {
    return {
      form: {
        areaCode: '+86',
        mobile: '',
        mobileCaptcha: '',
        newPassword: '',
        confirmPassword: ''
      },
      countdown: 0,
      timer: null
    }
  },
  mounted() {
    this.$store.dispatch('fetchPubConfig');
  },
  methods: {
    sendMobileCaptcha() {
      if (!validateMobile(this.form.mobile, this.form.areaCode)) {
        showDanger(this.$t('retrievePassword.inputCorrectMobile'));
        return;
      }

      if (this.timer) {
        clearInterval(this.timer);
        this.timer = null;
      }

      this.countdown = 60;
      this.timer = setInterval(() => {
        if (this.countdown > 0) {
          this.countdown--;
        } else {
          clearInterval(this.timer);
          this.timer = null;
        }
      }, 1000);

      Api.user.sendSmsVerification({
        phone: this.form.areaCode + this.form.mobile,
        captcha: '',
        captchaId: '',
      }, () => {
        showSuccess(this.$t('retrievePassword.captchaSendSuccess'));
      }, (err) => {
        showDanger(err.data.msg || this.$t('register.captchaSendFailed'));
        this.countdown = 0;
      });
    },

    retrievePassword() {
      if (!validateMobile(this.form.mobile, this.form.areaCode)) {
        showDanger(this.$t('retrievePassword.inputCorrectMobile'));
        return;
      }
      if (!this.form.mobileCaptcha) {
        showDanger(this.$t('retrievePassword.mobileCaptchaRequired'));
        return;
      }
      if (this.form.newPassword !== this.form.confirmPassword) {
        showDanger(this.$t('retrievePassword.passwordsNotMatch'));
        return;
      }

      let encryptedPassword;
      try {
        encryptedPassword = sm2Encrypt(this.sm2PublicKey, this.form.newPassword);
      } catch (error) {
        console.error('Error encrypting password:', error);
        showDanger(this.$t('sm2.encryptionFailed'));
        return;
      }

      Api.user.retrievePassword({
        phone: this.form.areaCode + this.form.mobile,
        password: encryptedPassword,
        code: this.form.mobileCaptcha,
        captchaId: '',
      }, () => {
        showSuccess(this.$t('retrievePassword.passwordUpdateSuccess'));
        goToPage('/login');
      }, (err) => {
        showDanger(err.data.msg || this.$t('message.error'));
      });
    },

    goToLogin() {
      goToPage('/login');
    }
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
  padding: 0;

  &:disabled {
    background: #c0c4cc;
    cursor: not-allowed;
  }
}
</style>
