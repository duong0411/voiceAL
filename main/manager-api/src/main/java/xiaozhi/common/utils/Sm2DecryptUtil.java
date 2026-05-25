package xiaozhi.common.utils;

import org.apache.commons.lang3.StringUtils;
import xiaozhi.common.constant.Constant;
import xiaozhi.common.exception.ErrorCode;
import xiaozhi.common.exception.RenException;
import xiaozhi.modules.security.service.CaptchaService;
import xiaozhi.modules.sys.service.SysParamsService;

/**
 * SM2解密和验证码验证工具类
 */
public class Sm2DecryptUtil {

    private static final int CAPTCHA_LENGTH = 5;

    /**
     * 解密 SM2 密文。未传图形验证码 ID 时，解密结果整段视为密码；否则前 5 位为验证码、后面为密码。
     */
    public static String decryptAndValidateCaptcha(String encryptedPassword, String captchaId,
            CaptchaService captchaService, SysParamsService sysParamsService) {
        String privateKeyStr = sysParamsService.getValue(Constant.SM2_PRIVATE_KEY, true);
        if (StringUtils.isBlank(privateKeyStr)) {
            throw new RenException(ErrorCode.SM2_KEY_NOT_CONFIGURED);
        }

        String decryptedContent;
        try {
            decryptedContent = SM2Utils.decrypt(privateKeyStr, encryptedPassword);
        } catch (Exception e) {
            throw new RenException(ErrorCode.SM2_DECRYPT_ERROR);
        }

        if (StringUtils.isBlank(decryptedContent)) {
            throw new RenException(ErrorCode.SM2_DECRYPT_ERROR);
        }

        captchaId = normalizeCaptchaId(captchaId);
        if (captchaId == null) {
            return decryptedContent;
        }

        if (decryptedContent.length() <= CAPTCHA_LENGTH) {
            throw new RenException(ErrorCode.SMS_CAPTCHA_ERROR);
        }

        String embeddedCaptcha = decryptedContent.substring(0, CAPTCHA_LENGTH);
        String actualPassword = decryptedContent.substring(CAPTCHA_LENGTH);

        if (!captchaService.validate(captchaId, embeddedCaptcha, true)) {
            throw new RenException(ErrorCode.SMS_CAPTCHA_ERROR);
        }

        return actualPassword;
    }

    /**
     * 将空串、null、undefined 等视为未使用图形验证码。
     */
    public static String normalizeCaptchaId(String captchaId) {
        captchaId = StringUtils.trimToNull(captchaId);
        if (captchaId == null) {
            return null;
        }
        if ("null".equalsIgnoreCase(captchaId) || "undefined".equalsIgnoreCase(captchaId)) {
            return null;
        }
        return captchaId;
    }
}
