<template>
  <div class="val-auth-page" @keyup.enter="login">
    <section class="val-auth-hero">
      <div class="val-auth-hero-inner">
        <BrandLogo size="lg" />
        <p class="val-auth-tagline">{{ $t('brand.tagline') }}</p>
        <div class="val-auth-features">
          <span v-for="(feat, i) in featureList" :key="i" class="val-feature-pill">{{ feat }}</span>
        </div>
        <div class="val-waveform" aria-hidden="true">
          <span v-for="n in 12" :key="n" class="val-wave-bar" />
        </div>
      </div>
    </section>

    <section class="val-auth-panel">
      <div class="val-auth-card">
        <div class="val-auth-card-header">
          <div class="val-auth-heading">
            <BrandLogo compact size="sm" />
            <h1 class="val-auth-title">{{ $t('login.title') }}</h1>
            <p class="val-auth-subtitle">{{ $t('login.welcome') }}</p>
          </div>
          <el-dropdown
            trigger="click"
            class="val-lang-switch"
            @visible-change="handleLanguageDropdownVisibleChange"
          >
            <span class="val-lang-btn">
              {{ currentLanguageText }}
              <i class="el-icon-arrow-down" :class="{ 'rotate-down': languageDropdownVisible }" />
            </span>
            <el-dropdown-menu slot="dropdown">
              <el-dropdown-item @click.native="changeLanguage('vi')">{{ $t('language.vi') }}</el-dropdown-item>
              <el-dropdown-item @click.native="changeLanguage('en')">{{ $t('language.en') }}</el-dropdown-item>
            </el-dropdown-menu>
          </el-dropdown>
        </div>

        <template v-if="!isMobileLogin">
          <div class="val-input-box">
            <i class="el-icon-user" />
            <el-input v-model="form.username" :placeholder="$t('login.usernamePlaceholder')" />
          </div>
        </template>
        <template v-else>
          <div class="val-input-box" style="padding-right: 0">
            <el-select v-model="form.areaCode" style="width: 110px; margin-right: 8px">
              <el-option
                v-for="item in mobileAreaList"
                :key="item.key"
                :label="`${item.name} (${item.key})`"
                :value="item.key"
              />
            </el-select>
            <el-input v-model="form.mobile" :placeholder="$t('login.mobilePlaceholder')" />
          </div>
        </template>

        <div class="val-input-box">
          <i class="el-icon-lock" />
          <el-input
            v-model="form.password"
            :placeholder="$t('login.passwordPlaceholder')"
            type="password"
            show-password
          />
        </div>

        <div class="val-auth-links">
          <span class="link" @click="goToForgetPassword">{{ $t('login.forgetPassword') }}</span>
        </div>

        <div class="val-primary-btn" @click="login">{{ $t('login.login') }}</div>

        <!-- Google OAuth Sign In -->
        <div class="val-oauth-divider">
          <span>{{ $t('login.or') }}</span>
        </div>
        <div class="val-oauth-container">
          <button class="val-google-btn" @click="triggerGoogleLogin">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 48 48" width="20" height="20">
              <path fill="#EA4335" d="M24 9.5c3.54 0 6.71 1.22 9.21 3.6l6.85-6.85C35.9 2.38 30.47 0 24 0 14.62 0 6.51 5.38 2.56 13.22l7.98 6.19C12.43 13.72 17.74 9.5 24 9.5z"/>
              <path fill="#4285F4" d="M46.98 24.55c0-1.57-.15-3.09-.38-4.55H24v9.02h12.94c-.58 2.96-2.26 5.48-4.78 7.18l7.73 6c4.51-4.18 7.09-10.36 7.09-17.65z"/>
              <path fill="#FBBC05" d="M10.53 28.59c-.48-1.45-.76-2.99-.76-4.59s.27-3.14.76-4.59l-7.98-6.19C.92 16.46 0 20.12 0 24c0 3.88.92 7.54 2.56 10.78l7.97-6.19z"/>
              <path fill="#34A853" d="M24 48c6.48 0 11.93-2.13 15.89-5.81l-7.73-6c-2.18 1.48-4.97 2.31-8.16 2.31-6.26 0-11.57-4.22-13.47-9.91l-7.98 6.19C6.51 42.62 14.62 48 24 48z"/>
            </svg>
            {{ $t('login.continueWithGoogle') }}
          </button>
        </div>

        <div class="val-register-block">
          <p class="val-no-account">{{ $t('login.noAccount') }}</p>
          <button type="button" class="val-secondary-btn" @click="goToRegister">
            {{ $t('login.registerAccount') }}
          </button>
        </div>

        <div v-if="enableMobileRegister" class="val-login-type">
          <el-tooltip :content="$t('login.mobileLogin')" placement="top">
            <el-button
              :type="isMobileLogin ? 'primary' : 'default'"
              icon="el-icon-mobile-phone"
              circle
              @click="switchLoginType('mobile')"
            />
          </el-tooltip>
          <el-tooltip :content="$t('login.usernameLogin')" placement="top">
            <el-button
              :type="!isMobileLogin ? 'primary' : 'default'"
              icon="el-icon-user"
              circle
              @click="switchLoginType('username')"
            />
          </el-tooltip>
        </div>
      </div>
    </section>

    <footer class="val-auth-footer">
      <version-footer />
    </footer>
  </div>
</template>

<script>
import Api from '@/apis/api';
import BrandLogo from '@/components/BrandLogo.vue';
import VersionFooter from '@/components/VersionFooter.vue';
import i18n, { changeLanguage } from '@/i18n';
import { goToPage, showDanger, showSuccess, sm2Encrypt, validateMobile } from '@/utils';
import { mapState } from 'vuex';

export default {
  name: 'login',
  components: { VersionFooter, BrandLogo },
  computed: {
    ...mapState({
      allowUserRegister: (state) => state.pubConfig.allowUserRegister,
      enableMobileRegister: (state) => state.pubConfig.enableMobileRegister,
      mobileAreaList: (state) => state.pubConfig.mobileAreaList,
      sm2PublicKey: (state) => state.pubConfig.sm2PublicKey,
      googleClientId: (state) => state.pubConfig.googleClientId,
    }),
    currentLanguage() {
      return i18n.locale || 'vi';
    },
    currentLanguageText() {
      return this.currentLanguage === 'en' ? this.$t('language.en') : this.$t('language.vi');
    },
    featureList() {
      return [
        this.$t('brand.feature.voice'),
        this.$t('brand.feature.agents'),
        this.$t('brand.feature.devices'),
      ];
    },
  },
  data() {
    return {
      form: {
        username: '',
        password: '',
        areaCode: '+86',
        mobile: '',
      },
      isMobileLogin: false,
      languageDropdownVisible: false,
    };
  },
  mounted() {
    localStorage.removeItem('pubConfig');
    this.$store.dispatch('fetchPubConfig').then(() => {
      this.isMobileLogin = this.enableMobileRegister;
      if (this.googleClientId) {
        this.loadGoogleSdk(() => {
          this.initGoogleSignIn();
        });
      }
    });
  },
  methods: {
    handleLanguageDropdownVisibleChange(visible) {
      this.languageDropdownVisible = visible;
    },
    changeLanguage(lang) {
      changeLanguage(lang);
      this.languageDropdownVisible = false;
      this.$message.success({ message: this.$t('message.success'), showClose: true });
    },
    switchLoginType(type) {
      this.isMobileLogin = type === 'mobile';
      this.form.username = '';
      this.form.mobile = '';
      this.form.password = '';
    },
    validateInput(input, messageKey) {
      if (!input.trim()) {
        showDanger(this.$t(messageKey));
        return false;
      }
      return true;
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
    async login() {
      if (this.isMobileLogin) {
        if (!validateMobile(this.form.mobile, this.form.areaCode)) {
          showDanger(this.$t('login.requiredMobile'));
          return;
        }
        this.form.username = this.form.areaCode + this.form.mobile;
      } else if (!this.validateInput(this.form.username, 'login.requiredUsername')) {
        return;
      }

      if (!this.validateInput(this.form.password, 'login.requiredPassword')) return;

      let encryptedPassword;
      try {
        encryptedPassword = sm2Encrypt(this.sm2PublicKey, this.form.password);
      } catch (error) {
        console.error("Password encryption failed:", error);
        showDanger(this.$t('sm2.encryptionFailed'));
        return;
      }

      Api.user.login(
        {
          username: this.form.username,
          password: encryptedPassword,
          captchaId: '',
        },
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
    goToRegister() {
      goToPage('/register');
    },
    goToForgetPassword() {
      goToPage('/retrieve-password');
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
    triggerGoogleLogin() {
      if (!this.googleClientId) {
        showDanger(this.$t('login.googleNotConfigured'));
        return;
      }
      this.loadGoogleSdk(() => {
        this.initGoogleSignIn();
        window.google.accounts.id.prompt((notification) => {
          if (notification.isNotDisplayed() || notification.isSkippedMoment()) {
            showDanger(this.$t('login.loginFailed'));
          }
        });
      });
    },
    initGoogleSignIn() {
      if (!window.google || !window.google.accounts) return;
      window.google.accounts.id.initialize({
        client_id: this.googleClientId,
        callback: this.handleGoogleCredentialResponse,
      });
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
};
</script>

<style lang="scss" scoped>
@import './auth.scss';
</style>
