
package vn.com.m1tech.cnn.appEtax.common; 

import java.text.Normalizer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Pattern;

import vn.com.m1tech.cnn.domain.admin.Setting;

public class Utils {

	public static Setting getSettingByName(String name, List<Setting> lst) {
		for (int i = 0; i < lst.size(); i++) {
			Setting item = lst.get(i);
			if (item.getName().equals(name))
				return item;
		}
		return null;

	}

	public static String parametersToJson(String parameters) {
		String json = "{\"";
		try {
			if (!"".equals(parameters)) {
				parameters = parameters.replace("\r", "");
				parameters = parameters.replace("\n", "");
				parameters = parameters.replace("+", " ");
				parameters = parameters.replace("+=", "=");
				parameters = parameters.replace("&", "\",\"");
				parameters = parameters.replace("=", "\":\"");
				json += parameters;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		json += "\"}";
		return json;
	}

	public static String removeCharacter(String parameters) {
		String json = "";
		try {
			if (!"".equals(parameters)) {
				parameters = parameters.replace("\r", "");
				parameters = parameters.replace("\n", "");
				parameters = parameters.replace("\t", "");
				json += parameters;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		json += "";
		return json;
	}

	public static boolean checkIsNullOrEmpty(String s) {
		if (s == null || s.trim().equals(""))
			return true;

		return false;

	}

	public static boolean isNumeric(String str) {
		try {
			Double.parseDouble(str);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	public static boolean isLong(String str) {
		try {
			Long.parseLong(str);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	public static String now(String dateFormat) {
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
		return sdf.format(cal.getTime());
	}

	public static String getSDateYYYYMMDDHHmmssSSSS() {
		Date dLocalDate = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssSSSS");
		return sdf.format(dLocalDate);
	}

	public static String getSDateHHmmssSSS() {
		Date dLocalDate = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("HHmmssSSS");
		return sdf.format(dLocalDate);
	}

	public static String getStringDate(Date date, String format) {
		SimpleDateFormat dateFormat = new SimpleDateFormat(format);
		return dateFormat.format(date);
	}
	
	public static String convertMMYYYYtoFormat(String dateStr, String format) throws ParseException {
	    SimpleDateFormat inputFormat = new SimpleDateFormat("MM/yyyy");
	    SimpleDateFormat outputFormat = new SimpleDateFormat(format);
	    Date date = inputFormat.parse(dateStr);
	    return outputFormat.format(date);
	}

	public static String getSDateYYYYMMDDHHmmss(Date date) {
		Date dLocalDate = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
		return sdf.format(dLocalDate);
	}

	public static String toKhongDau(String str) {
		String nfdNormalizedString = Normalizer.normalize(str, Normalizer.Form.NFD);
		Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
		return pattern.matcher(nfdNormalizedString).replaceAll("").replaceAll("Ä?", "D").replaceAll("Ä‘", "d");
	}
	
	public static String getSDateTimeZone() {
		Date dLocalDate = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US);
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
		return sdf.format(dLocalDate);
	}

	public static String removeCharacterHtml(String parameters) {
		String json = "";
		try {
			if (!checkIsNullOrEmpty(parameters)) {
				for (int i = 0; i < parameters.length(); i++) {
					int ascii = parameters.charAt(i);
					if (ascii == 8208 || ascii == 8209 || ascii == 8210 || ascii == 8211 || ascii == 8212
							|| ascii == 8213) {
						parameters = parameters.replace((char) ascii, '-');
					} else if (ascii == 8192 || ascii == 8193 || ascii == 8194 || ascii == 8195 || ascii == 8196
							|| ascii == 8197 || ascii == 8198 || ascii == 8199 || ascii == 8200 || ascii == 8201
							|| ascii == 8202 || ascii == 8203 || ascii == 8204 || ascii == 8205 || ascii == 8206
							|| ascii == 8207) {
						parameters = parameters.replace((char) ascii, (char) 0 );
					}

					else if(ascii == 8236 || ascii == 8232 || ascii == 8233 || ascii == 8234 || ascii == 8235
							|| ascii == 8237 || ascii == 8238 || ascii == 8239
							|| ascii == 8287 || ascii == 8288 || ascii == 8289
							|| ascii == 8290 || ascii == 8291 || ascii == 8292
							|| ascii == 8293 || ascii == 8294 || ascii == 8295
							|| ascii == 8296 || ascii == 8297 || ascii == 8298
							|| ascii == 8299 || ascii == 8300 || ascii == 8301
							|| ascii == 8303 || ascii == 8303) {
						parameters = parameters.replace((char) ascii, (char) 0);  
					}
					else if(ascii == 160){
						parameters = parameters.replace((char) 160, ' ');
					}
					
				}
				return parameters = parameters.replaceAll("\\s+"," ");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return json;
	}
	
	public static String removeCharacterHtmlAccountNumber(String parameters) {
		String json = "";
		try {
			if (!checkIsNullOrEmpty(parameters)) {
				for (int i = 0; i < parameters.length(); i++) {
					int ascii = parameters.charAt(i);
					if (ascii == 8208 || ascii == 8209 || ascii == 8210 || ascii == 8211 || ascii == 8212
							|| ascii == 8213) {
						parameters = parameters.replace((char) ascii, '-');
					} else if (ascii == 8192 || ascii == 8193 || ascii == 8194 || ascii == 8195 || ascii == 8196
							|| ascii == 8197 || ascii == 8198 || ascii == 8199 || ascii == 8200 || ascii == 8201
							|| ascii == 8202 || ascii == 8203 || ascii == 8204 || ascii == 8205 || ascii == 8206
							|| ascii == 8207) {
						parameters = parameters.replace((char) ascii, (char) 0);
					}

					else if(ascii == 8236 || ascii == 8232 || ascii == 8233 || ascii == 8234 || ascii == 8235
							|| ascii == 8237 || ascii == 8238 || ascii == 8239
							|| ascii == 8287 || ascii == 8288 || ascii == 8289
							|| ascii == 8290 || ascii == 8291 || ascii == 8292
							|| ascii == 8293 || ascii == 8294 || ascii == 8295
							|| ascii == 8296 || ascii == 8297 || ascii == 8298
							|| ascii == 8299 || ascii == 8300 || ascii == 8301
							|| ascii == 8303 || ascii == 8303) {
						parameters = parameters.replace((char) ascii, (char) 0);    
					}
					else if(ascii == 160){
						parameters = parameters.replace((char) 160, ' ');
					}
					
				}
				return parameters = parameters.replaceAll("\\s+"," ");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return json;
	}
}
