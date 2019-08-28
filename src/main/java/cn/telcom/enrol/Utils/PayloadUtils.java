package cn.telcom.enrol.Utils;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * 1vs1payload参数拼接
 * @author kenny_peng
 * @created 2019/7/25 14:13
 */
public class PayloadUtils {

    private static String type_application = "text-independent";
    private static String mode_application = "ti_plp2covv2";
    private static String samplingRate_application = "8000";
    private static String format_application = "pcm16";

    private static String format = "pcm16";
    private static String samplingRate = "8000";

    /**
     * 生成注册用payload
     * @param name_id
     * @param audio_base64
     * @return
     */
    public static String identifyPayload(String name_id,String audio_base64){
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("userId", name_id);
        jsonObject.put("format", format);
        jsonObject.put("samplingRate", samplingRate);
        jsonObject.put("audioData", audio_base64);
        jsonObject.put("detectReplay", "false");
        return jsonObject.toString();
    }


    public static String enrolOrVerifyPayload(String log_id,String name_id,String audio_base64){
        String loggingId = log_id;
        String identifier = name_id;
        String type = type_application;
        String mode = mode_application;
        String samplingRate = samplingRate_application;
        String format = format_application;
        String usage_context = "default";
        String replay = "default";
        String base64 = audio_base64;

        JSONObject jsonObject = new JSONObject();
        /**
         * serviceData
         */

        // serviceData
        JSONObject jsonObject_ser = new JSONObject();
        jsonObject_ser.put("loggingId", loggingId);

        /**
         * userData
         */
        // userData
        JSONObject jsonObject_user = new JSONObject();
        jsonObject_user.put("identifier", identifier);

        /**
         * processingInformation
         */
        JSONObject jsonObject_pro = new JSONObject();

        /**
         * processingInformation biometric
         */
        // processingInformation_biometric
        JSONObject jsonObject_pro_bio = new JSONObject();
        jsonObject_pro_bio.put("type", type);
        jsonObject_pro_bio.put("mode", mode);
        jsonObject_pro.put("biometric", jsonObject_pro_bio);

        /**
         * processingInformation audioCharacteristics
         */
        // processingInformation_audioCharacteristics
        JSONObject jsonObject_pro_audio = new JSONObject();
        jsonObject_pro_audio.put("samplingRate", samplingRate);
        jsonObject_pro_audio.put("format", format);
        jsonObject_pro.put("audioCharacteristics", jsonObject_pro_audio);

        /**
         * processingInformation metaInformation
         */
        // processingInformation

        // processingInformation_metaInformation
        JSONArray jsonArray = new JSONArray();

        // processingInformation_metaInformation_usage-context
        JSONObject jsonObject_pro_met_usa = new JSONObject();
        jsonObject_pro_met_usa.put("key", "usage-context");

        // processingInformation_metaInformation_usage-context_value
        JSONObject jsonObjectjsonObject_pro_met_usa_val = new JSONObject();
        jsonObjectjsonObject_pro_met_usa_val.put("value", usage_context);
        jsonObjectjsonObject_pro_met_usa_val.put("encrypted", "false");
        jsonObject_pro_met_usa.put("value", jsonObjectjsonObject_pro_met_usa_val);
        // processingInformation_metaInformation_detect-replay-v2-16k
        jsonArray.add(jsonObject_pro_met_usa);

        // processingInformation_metaInformation_detect-replay-v2-16k
        JSONObject jsonObject_pro_met_det = new JSONObject();
        jsonObject_pro_met_det.put("key", "detect-replay-v2-8k");

        // processingInformation_metaInformation_detect-replay-v2-16k_value
        JSONObject jsonObject_pro_met_det_val = new JSONObject();
        jsonObject_pro_met_det_val.put("value", replay);
        jsonObject_pro_met_det_val.put("encrypted", "false");
        jsonObject_pro_met_det.put("value", jsonObject_pro_met_det_val);
        jsonArray.add(jsonObject_pro_met_det);

        // processingInformation_metaInformation_get-snr
        JSONObject jsonObject_get_snr = new JSONObject();
        jsonObject_get_snr.put("key", "get-snr");

        // processingInformation_metaInformation_detect-replay-v2-16k_value
        JSONObject jsonObject_get_snr_val = new JSONObject();
        jsonObject_get_snr_val.put("value", "10");
        jsonObject_get_snr_val.put("encrypted", "false");
        jsonObject_get_snr.put("value", jsonObject_get_snr_val);
        jsonArray.add(jsonObject_get_snr);

        /**
         * processingInformation
         */
        jsonObject_pro.put("biometric", jsonObject_pro_bio);
        jsonObject_pro.put("audioCharacteristics", jsonObject_pro_audio);
        jsonObject_pro.put("metaInformation", jsonArray);

        /*
         * audioInput
         */
        // audioInput_audio
        JSONObject jsonObject_aud_aud = new JSONObject();
        jsonObject_aud_aud.put("base64", base64);

        // audioInput
        JSONObject jsonObject_aud = new JSONObject();
        jsonObject_aud.put("secondsThreshold", "0");
        jsonObject_aud.put("audio", jsonObject_aud_aud);

        /**
         * all put
         */

        jsonObject.put("audioInput", jsonObject_aud);
        jsonObject.put("userData", jsonObject_user);
        jsonObject.put("serviceData", jsonObject_ser);
        jsonObject.put("processingInformation", jsonObject_pro);

        return jsonObject.toString();
    }


    public static String isSpeakerEnrolledOrDeleteSpeakerPayload(String log_id,String name_id){
        String loggingId = log_id;
        String identifier = name_id;
        String type = type_application;
        String mode = mode_application;
        String samplingRate = samplingRate_application;
        String format = format_application;
        String usage_context = "default";
        String replay = "default";

        JSONObject jsonObject = new JSONObject();
        /**
         * serviceData
         */

        // serviceData
        JSONObject jsonObject_ser = new JSONObject();
        jsonObject_ser.put("loggingId", loggingId);

        /**
         * userData
         */
        // userData
        JSONObject jsonObject_user = new JSONObject();
        jsonObject_user.put("identifier", identifier);

        /**
         * processingInformation
         */
        JSONObject jsonObject_pro = new JSONObject();

        /**
         * processingInformation biometric
         */
        // processingInformation_biometric
        JSONObject jsonObject_pro_bio = new JSONObject();
        jsonObject_pro_bio.put("type", type);
        jsonObject_pro_bio.put("mode", mode);
        jsonObject_pro.put("biometric", jsonObject_pro_bio);

        /**
         * processingInformation audioCharacteristics
         */
        // processingInformation_audioCharacteristics
        JSONObject jsonObject_pro_audio = new JSONObject();
        jsonObject_pro_audio.put("samplingRate", samplingRate);
        jsonObject_pro_audio.put("format", format);
        jsonObject_pro.put("audioCharacteristics", jsonObject_pro_audio);

        /**
         * processingInformation metaInformation
         */
        // processingInformation

        // processingInformation_metaInformation
        JSONArray jsonArray = new JSONArray();

        // processingInformation_metaInformation_usage-context
        JSONObject jsonObject_pro_met_usa = new JSONObject();
        jsonObject_pro_met_usa.put("key", "usage-context");

        // processingInformation_metaInformation_usage-context_value
        JSONObject jsonObjectjsonObject_pro_met_usa_val = new JSONObject();
        jsonObjectjsonObject_pro_met_usa_val.put("value", usage_context);
        jsonObjectjsonObject_pro_met_usa_val.put("encrypted", "false");
        jsonObject_pro_met_usa.put("value", jsonObjectjsonObject_pro_met_usa_val);
        // processingInformation_metaInformation_detect-replay-v2-16k
        jsonArray.add(jsonObject_pro_met_usa);
        jsonObject_pro.put("metaInformation",jsonArray);

        /**
         * all put
         */

//        jsonObject.put("audioInput", jsonObject_aud);
        jsonObject.put("userData", jsonObject_user);
        jsonObject.put("serviceData", jsonObject_ser);
        jsonObject.put("processingInformation", jsonObject_pro);


        return jsonObject.toString();

    }



    public static void main(String[] args) {
        isSpeakerEnrolledOrDeleteSpeakerPayload("123","123");
    }

}
