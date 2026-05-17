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

        <div class="val-captcha-row">
          <div class="val-input-box">
            <i class="el-icon-key" />
            <el-input v-model="form.captcha" :placeholder="$t('login.captchaPlaceholder')" />
          </div>
          <div
            v-if="captchaUrl"
            class="val-captcha-wrap"
            :title="$t('login.captchaRefresh')"
            @click="fetchCaptcha"
          >
            <img
              :src="captchaUrl"
              class="val-captcha-img"
              :alt="$t('login.captchaPlaceholder')"
            />
          </div>
        </div>

        <div class="val-auth-links">
          <span v-if="enableMobileRegister" class="link" @click="goToForgetPassword">{{ $t('login.forgetPassword') }}</span>
        </div>

        <div class="val-primary-btn" @click="login">{{ $t('login.login') }}</div>

        <div v-if="allowUserRegister" class="val-register-block">
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

        <p class="val-legal">
          {{ $t('login.agreeTo') }}
          <span class="link" @click="openPage('/user-agreement.html')">{{ $t('login.userAgreement') }}</span>
          {{ $t('login.and') }}
          <span class="link" @click="openPage('/privacy-policy.html')">{{ $t('login.privacyPolicy') }}</span>
        </p>
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
import { getUUID, goToPage, showDanger, showSuccess, sm2Encrypt, validateMobile } from '@/utils';
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
        captcha: '',
        captchaId: '',
        areaCode: '+86',
        mobile: '',
      },
      captchaUuid: '',
      captchaUrl: '',
      isMobileLogin: false,
      languageDropdownVisible: false,
    };
  },
  mounted() {
    this.fetchCaptcha();
    localStorage.removeItem('pubConfig');
    this.$store.dispatch('fetchPubConfig').then(() => {
      this.isMobileLogin = this.enableMobileRegister;
    });
  },
  methods: {
    openPage(url) {
      const lang = this.$i18n ? this.$i18n.locale : 'vi';
      if (lang === 'en') {
        url = url.replace('.html', '-en.html');
      }
      window.open(url, '_blank');
    },
    fetchCaptcha() {
      const token = localStorage.getItem('token');
      if (token) {
        if (this.$route.path !== '/home') {
          this.$router.push('/home');
        }
        return;
      }
      this.captchaUuid = getUUID();
      Api.user.getCaptcha(this.captchaUuid, (res) => {
        if (res.status === 200) {
          const blob = new Blob([res.data], { type: res.data.type });
          this.captchaUrl = URL.createObjectURL(blob);
        } else {
          showDanger(this.$t('login.captchaLoadFailed'));
        }
      });
    },
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
      this.form.captcha = '';
      this.fetchCaptcha();
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
      if (!this.validateInput(this.form.captcha, 'login.requiredCaptcha')) return;

      let encryptedPassword;
      try {
        encryptedPassword = sm2Encrypt(this.sm2PublicKey, this.form.captcha + this.form.password);
      } catch (error) {
        showDanger(this.$t('sm2.encryptionFailed'));
        return;
      }

      this.form.captchaId = this.captchaUuid;
      Api.user.login(
        {
          username: this.form.username,
          password: encryptedPassword,
          captchaId: this.form.captchaId,
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

      setTimeout(() => this.fetchCaptcha(), 1000);
    },
    goToRegister() {
      goToPage('/register');
    },
    goToForgetPassword() {
      goToPage('/retrieve-password');
    },
  },
};
</script>

<style lang="scss" scoped>
@import './auth.scss';
</style>
