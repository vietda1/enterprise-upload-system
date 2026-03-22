package vn.com.m1tech.cnn.appEtax.common;

public class Constants {

	public static final String JUPITER_APP = "JPT";
	public static final String JUPITER_TF_COLLECT_TOTAL = "JPT-TS-CA_GL";
	public static final String JUPITER_TF_SERVICE_FEE = "JPT-TS-SERVICE-FEE COLLECTION";
	public static final String JUPITER_TF_TRANFER_FEE = "JPT-TS-TRANFER-FEE COLLECTION";

	public static final String ESB_SUCCESS = "0";
	public static final String SystemOk = "System OK";

	public static final String SOURCE_TKTT = "0";
	public static final String SOURCE_TM = "1";
	public static final String SOURCE_OTHER = "2";

	public static final String SOURCE_TKTT_STR = "TK TT của KH";
	public static final String SOURCE_TM_STR = "Ti�?n mặt";
	public static final String SOURCE_OTHER_STR = "Khác";

	public static final String NHH = "NHH:";

	public static final Long STATUS_NEW = 0L;
	public static final Long STATUS_APPROVE = 1L;
	public static final Long STATUS_REJECT = 2L;
	public static final Long STATUS_RE_APPROVE = 3L;

	public static final String FILE_TYPE_PDF = "1";
	public static final String FILE_TYPE_EXCEL = "0";

	public static final String ESB_RESPONSE_0_STR = "Thành công";
	public static final String ESB_RESPONSE_NOT_0_STR = "Lỗi";

	public static final String ESB_RESPONSE_0 = "0";

	public static final String ESB_RESPONSE_ERROR_1 = "Lỗi giao dịch ESB";
	public static final String ESB_RESPONSE_ERROR_2 = "ESB trả v�? null";

	public static final Long BATCH_STATUS_NEW = 0L;
	public static final Long BATCH_STATUS_APPROVE = 1L;
	public static final Long BATCH_STATUS_REJECT = 2L;
	public static final Long BATCH_STATUS_ERROR = 3L;

	public static final String BATCH_STATUS_NEW_STR = "Ch�? phê duyệt";
	public static final String BATCH_STATUS_APPROVED_STR = "�?ã phê duyệt";
	public static final String BATCH_STATUS_REJJECT_STR = "Từ chối";
	public static final String BATCH_STATUS_ERROR_STR = "Lỗi";

	public static final String STATUS_NEW_STR = "Ch�? duyệt";
	public static final String STATUS_APPROVE_STR = "�?ã duyệt";
	public static final String STATUS_RE_APPROVE_STR = "�?ã duyệt lại";
	public static final String STATUS_REJECT_STR = "Từ chối";

	public static final String SELL_RATE = "10000000";
	public static final String BUY_RATE = "10000000";
	public static final String VAT_FEE = "0";
	public static final String SERVICE_FEE = "0";
	public static final String SYS_MODE_DAY = "DAY";
	public static final String IS_OW_FEE = "Y";

	public static final String TYPE_TRANFER_1 = "1";
	public static final String TYPE_TRANFER_2 = "2";
	public static final String TYPE_TRANFER_3 = "3";

	public static final String BANK_INTER_1 = "MSB";
	public static final String BANK_INTER_2 = "HANG HAI";
	public static final String BANK_INTER_3 = "MARITIMEBANK";
	public static final String BANK_INTER_4 = "MARITIME BANK";

	public static final String BANK_IN = "Nội bộ MSB";
	public static final String BANK_OUT = "�?i CITAD";

	public static final String BANK_IN_NUM = "0";
	public static final String BANK_OUT_NUM = "1";

	public static final String BANK_TYPE_NHNN = "0";
	public static final String BANK_TYPE_KBNN = "1";
	public static final String BANK_TYPE_NHDT = "2";
	public static final String BANK_TYPE_OTHER = "3";
	public static final String BANK_TYPE_NHTT = "4";
	public static final String BANK_TYPE_NHTT_OTHER = "5";

	public static final String VND_CURRENCY = "VND";
	public static final String CMT_ACCOUNT = "CMT";

	public static final String CREDIT_RATE = "10000000";
	public static final String DEBIT_RATE = "10000000";

	public static final String KBNN1 = "KBNN";
	public static final String KBNN2 = "KHO BAC NHA NUOC";
	public static final String KBNN3 = "KB NHA NUOC";

	public static final String CSXH1 = "CHINH SACH XA HOI";
	public static final String CSXH2 = "CSXH";
	public static final String CSXH3 = "CHINH SACH XH";
	public static final String CSXH4 = "CS XH";
	public static final String CSXH5 = "CS XA HOI";

	public static final String PTVN1 = "PHAT TRIEN VN";
	public static final String PTVN2 = "PHAT TRIEN VIET NAM";
	public static final String PTVN3 = "PTVN";

	public static final String NHNN1 = "NHNN";
	public static final String NHNN2 = "NGAN HANG NHA NUOC";
	public static final String NHNN3 = "NH NHA NUOC";

	public static final String TRANFER_SALARY_GL_TG = "TRANFER_SALARY_GL_TG";
	public static final String TRANFER_SALARY_ESB_TELLER_APPROVE_GL = "TRANFER_SALARY_ESB_TELLER_APPROVE_GL";
	public static final String TRANFER_SALARY_TRANCODE_COLLECT_FEE_GL = "TRANFER_SALARY_TRANCODE_COLLECT_FEE_GL";
	public static final String TRANFER_SALARY_CHARACTER_SPECIAL = "TRANFER_SALARY_CHARACTER_SPECIAL";
	public static final String TRANFER_SALARY_MAX_RECORD_EXPORT = "TRANFER_SALARY_MAX_RECORD_EXPORT";
	public static final String TRANFER_SALARY_MAX_RECORD_UPLOAD = "TRANFER_SALARY_MAX_RECORD_UPLOAD";
	public static final String TRANFER_SALARY_ESB_CHANNEL = "TRANFER_SALARY_ESB_CHANNEL";
	public static final String TRANFER_SALARY_ESB_HOST_NAME = "TRANFER_SALARY_ESB_HOST_NAME";
	public static final String TRANFER_SALARY_ESB_AUTHORIZER = "TRANFER_SALARY_ESB_AUTHORIZER";
	public static final String TRANFER_SALARY_ESB_PASSWORD = "TRANFER_SALARY_ESB_PASSWORD";
	public static final String TRANFER_SALARY_ESB_APP_REQ = "TRANFER_SALARY_ESB_APP_REQ";
	public static final String TRANFER_SALARY_ESB_LINK = "TRANFER_SALARY_ESB_LINK";
	public static final String TRANFER_SALARY_TRAN_CODE_GET_CASA = "TRANFER_SALARY_TRAN_CODE_GET_CASA";
	public static final String TRANFER_SALARY_ESB_TELLER_APPROVE = "TRANFER_SALARY_ESB_TELLER_APPROVE";

	public static final String TRANFER_SALARY_GL_ACCOUNT_SERVICE_FEE = "TRANFER_SALARY_GL_ACCOUNT_SERVICE_FEE";
	public static final String TRANFER_SALARY_GL_ACCOUNT_TRANFER_FEE = "TRANFER_SALARY_GL_ACCOUNT_TRANFER_FEE";
	public static final String TRANFER_SALARY_TRANCODE_GL_CA = "TRANFER_SALARY_TRANCODE_GL_CA";
	public static final String TRANFER_SALARY_TRANCODE_GL_NOSTRO = "TRANFER_SALARY_TRANCODE_GL_NOSTRO";

	public static final String TRANFER_SALARY_TRANCODE_CA_GL = "TRANFER_SALARY_TRANCODE_CA_GL";
	public static final String TRANFER_SALARY_TRANCODE_COLLECT_FEE = "TRANFER_SALARY_TRANCODE_COLLECT_FEE";

	public static final String TRANFER_SALARY_FROM_LOW_TIME = "TRANFER_SALARY_FROM_LOW_TIME";
	public static final String TRANFER_SALARY_TO_LOW_TIME = "TRANFER_SALARY_TO_LOW_TIME";
	public static final String TRANFER_SALARY_FROM_HIGH_TIME = "TRANFER_SALARY_FROM_HIGH_TIME";
	public static final String TRANFER_SALARY_TO_HIGH_TIME = "TRANFER_SALARY_TO_HIGH_TIME";
	public static final String TRANFER_SALARY_FROM_CUTOFF_TIME = "TRANFER_SALARY_FROM_CUTOFF_TIME";
	public static final String TRANFER_SALARY_TO_CUTOFF_TIME = "TRANFER_SALARY_TO_CUTOFF_TIME";

	public static final String TRANFER_SALARY_FROM_LOW_TIME_END_MONTH = "TRANFER_SALARY_FROM_LOW_TIME_END_MONTH";
	public static final String TRANFER_SALARY_TO_LOW_TIME_END_MONTH = "TRANFER_SALARY_TO_LOW_TIME_END_MONTH";
	public static final String TRANFER_SALARY_FROM_HIGH_TIME_END_MONTH = "TRANFER_SALARY_FROM_HIGH_TIME_END_MONTH";
	public static final String TRANFER_SALARY_TO_HIGH_TIME_END_MONTH = "TRANFER_SALARY_TO_HIGH_TIME_END_MONTH";
	public static final String TRANFER_SALARY_FROM_CUTOFF_TIME_END_MONTH = "TRANFER_SALARY_FROM_CUTOFF_TIME_END_MONTH";
	public static final String TRANFER_SALARY_TO_CUTOFF_TIME_END_MONTH = "TRANFER_SALARY_TO_CUTOFF_TIME_END_MONTH";
	public static final String TRANFER_SALARY_DAY_END_MONTH = "TRANFER_SALARY_DAY_END_MONTH";

	public static final String TRANFER_SALARY_GL_LOW_TIME = "TRANFER_SALARY_GL_LOW_TIME";
	public static final String TRANFER_SALARY_GL_HIGH_TIME = "TRANFER_SALARY_GL_HIGH_TIME";
	public static final String TRANFER_SALARY_GL_CUTOFF_TIME = "TRANFER_SALARY_GL_CUTOFF_TIME";

	public static final String TRANFER_SALARY_AMOUNT_HIGHT = "TRANFER_SALARY_AMOUNT_HIGHT";

	public static final String TRANFER_SALARY_SIBS_HOST = "TRANFER_SALARY_SIBS_HOST";
	public static final String TRANFER_SALARY_SIBS_USER = "TRANFER_SALARY_SIBS_USER";
	public static final String TRANFER_SALARY_SIBS_PASS = "TRANFER_SALARY_SIBS_PASS";
	public static final String TRANFER_SALARY_SIBS_LIB_DAT = "TRANFER_SALARY_SIBS_LIB_DAT";

	public static final String STT_EMPTY = "Không được để trống STT";
	public static final String RECEIVER_NAME_EMPTY = "Không được để trống TÊN �?ƠN VỊ HƯỞNG";
	public static final String RECEIVER_NAME_MAX_LENGTH = "�?ộ dài TÊN �?ƠN VỊ HƯỞNG lớn hơn 70 ký tự";

	public static final String RECEIVER_BANK_EMPTY = "Không được để trống NGÂN HÀNG HƯỞNG";

	public static final String RECEIVER_BANK_IN_OUT = "Không được đi trong hay ngoài hệ thống";

	public static final String RECEIVER_ACCOUNT_NUMBER_EMPTY = "Không được để trống S�? TÀI KHOẢN HƯỞNG";

	public static final String ACCOUNT_NUMBER_MSB_NOT_VALID = "Số tài khoản MSB không hợp lệ";

	public static final String ACCOUNT_NUMBER_MSB_MAX_LENGTH = "Số tài khoản MSB lớn hơn 14 ký tự";

	public static final String ACCOUNT_NUMBER_OUT_MSB_MAX_LENGTH = "Số tài khoản ngoài MSB lớn hơn 20 ký tự";

	public static final String AMOUNT_EMPTY = "Không được để trống S�? TIỀN";

	public static final String AMOUNT_NOT_VALID = "Số ti�?n không hợp lệ";

	public static final String AMOUNT_LOWER_0 = "Số ti�?n không được nh�? hơn hoặc bằng 0";

	public static final String DESCRITION_MAX_LENGTH = "DIỄN GIẢI CHI TIẾT không được lớn hơn 190 ký tự";

	public static final String PRE_COLLECT_FEE = "THU PHI CHI HO TL ";
	public static final String PRE_COLLECT_TRANFER = "THU PHI CHUYEN TIEN TL ";

	public static final String RECEIVER_NAME_CHARACTER_SPECIAL = "TÊN �?ƠN VỊ HƯỞNG không được chứa ký tự đặt biệt";

	public static final String RECEIVER_BANK_NAME_CHARACTER_SPECIAL = "TÊN NGÂN HÀNG HƯỞNG không được chứa ký tự đặt biệt";

	public static final String DESCRITION_CHARACTER_SPECIAL = "DIỄN GIẢI CHI TIẾT không được chứa ký tự đặt biệt";

	public static final String ACCOUNT_NUMBER_CHARACTER_SPECIAL = "S�? TÀI KHOẢN NGƯỜI HƯỞNG không được chứa ký tự đặt biệt";

	public static final String KBNN_MSG_1 = "NGÂN HÀNG HƯỞNG là kho bạc";

	public static final String KBNN_MSG_2 = "Không tìm thấy thông tin mã CITAD trong KBNN";

	public static final String NHNN_MSG_1 = "Không tìm thấy thông tin mã CITAD trong NHNN";

	public static final String NHH_DESCRIPTION_MAX_LENGTH = "Nội dung gồm NGÂN HÀNG HƯỞNG và DIỄN GIẢI vượt quá 190 ký tự";

	public static final String TKTT_MSG_1 = "TÀI KHOẢN NGƯỜI KHÔNG CƯ TRÚ/ NGƯỜI NƯỚC NGOÀI  �?Ề NGHỊ KIỂM TRA HỒ SƠ KH�?CH HÀNG";

	public static final String TKTT_MSG_2 = "Tài khoản thanh toán không hợp lệ, trạng thái tài khoản không hợp lệ";

	public static final String TKTT_MSG_3 = "Tài khoản thanh toán không hợp lệ, tên tài khoản trên file không giống";

	public static final String TKTT_MSG_4 = "Tài khoản thanh toán không hợp lệ, tài khoản không phải VND";

	public static final String TKTT_MSG_5 = "Tài khoản thanh toán không hợp lệ, không tìm thấy tài khoản";

	public static final String TKTT_MSG_NEW_1 = "Tài khoản mở mới: TÀI KHOẢN NGƯỜI KHÔNG CƯ TRÚ/ NGƯỜI NƯỚC NGOÀI  �?Ề NGHỊ KIỂM TRA HỒ SƠ KH�?CH HÀNG";

	public static final String TKTT_MSG_NEW_2 = "Tài khoản mở mới: Tài khoản thanh toán không hợp lệ, trạng thái tài khoản không hợp lệ";

	public static final String TKTT_MSG_NEW_3 = "Tài khoản mở mới: Tài khoản thanh toán không hợp lệ, tên tài khoản trên file không giống";

	public static final String TKTT_MSG_NEW_4 = "Tài khoản mở mới: Tài khoản thanh toán không hợp lệ, tài khoản không phải VND";

	public static final String TOTAL_AMOUNT_MSG_1 = "Tổng ti�?n từng dòng và Tổng cộng số ti�?n không bằng nhau";

	public static final String TOTAL_AMOUNT_MSG_2 = "Tổng số ti�?n từng dòng hợp lệ";

	public static final String VALID_STATUS_0 = "0";

	public static final String VALID_STATUS_1 = "1";

	public static final String UPLOAD_FINISH = "KIỂM TRA FILE HOÀN THÀNH!";

	public static final String UPLOAD_ERROR = "Có LỖI xảy ra khi upload lô! ";

	public static final String UPLOAD_ERROR_1 = "Có LỖI xảy ra khi upload lô! File excel KHÔNG HỢP LỆ!";

	public static final String UPLOAD_ERROR_2 = "Có LỖI xảy ra khi upload lô! Số lượng bản ghi lớn hơn:";

	public static final String UPLOAD_ERROR_3 = "Có LỖI xảy ra khi upload lô! File excel không có dòng TỔNG CỘNG.";

	public static final String UPLOAD_ERROR_4 = "KIỂM TRA FILE LỖI, File không đúng định dạng xlsx or xls!";

	public static final String UPLOAD_ERROR_5 = "KIỂM TRA FILE LỖI, Lỗi ngoại lệ: ";

	public static final String DICTIONARY_1 = "Từ điển: ";

	public static final String DICTIONARY_2 = "Tìm thấy trong từ điển: ";

	public static final String DICTIONARY_3 = "Lỗi không tìm được mã CITAD do không có tỉnh/thành hoặc tỉnh/thành không đúng với từ điển";

	public static final String NOT_FOUND_NHCL_NHTT = "Không tìm thấy mã CITAD trong bảng mapping NHCL và NHTT";

	public static final String NHCL_MSG_1 = "Tìm thấy mã CITAD ở trong cột NAME NGÂN HÀNG CÒN LẠI";

	public static final String NHTT_MSG_1 = "Tìm thấy mã CITAD ở trong cột NAME NGÂN HÀNG TẬP TRUNG";

	public static final String NHTT_MSG_2 = "Lỗi tìm thấy hơn 1 từ điển trong bảng mapping NHTT: ";

	public static final String NHTT_MSG_3 = "Tìm thấy mã CITAD theo từ điển NHTT: ";

	public static final String RECORD_BEFORE_DATE = "KHÔNG �?ƯỢC DUYỆT BẢN GHI QUA NGÀY";

	public static final String TRANFER_SALARY_HIGH_TIME_011 = "TRANFER_SALARY_HIGH_TIME_011";

	public static final String TRANFER_SALARY_CUTOFF_TIME_011 = "TRANFER_SALARY_CUTOFF_TIME_011";

	public static final String CODE_99999 = "99999";

	public static final String DESC_99999 = "KHONG TRA VE KET QUA";

	public static final String TRANFER_SALARY_MAX_ROW_INQUIRY = "TRANFER_SALARY_MAX_ROW_INQUIRY";
	public static final String TRANFER_SALARY_T24 = "TRANFER_SALARY_T24";
	public static final String TRANFER_SALARY_T24_AUTHORIZE = "TRANFER_SALARY_T24_AUTHORIZE";
	public static final String TRANFER_SALARY_T24_PASSWORD = "TRANFER_SALARY_T24_PASSWORD";
	public static final String TRANFER_SALARY_T24_REQ_APP = "TRANFER_SALARY_T24_REQ_APP";
	public static final String TRANFER_SALARY_T24_CHANNEL = "TRANFER_SALARY_T24_CHANNEL";
	public static final String TRANFER_SALARY_T24_HOST_NAME = "TRANFER_SALARY_T24_HOST_NAME";
	public static final String TRANFER_SALARY_T24_SRV038_LINK = "TRANFER_SALARY_T24_SRV038_LINK";
	public static final String TRANFER_SALARY_T24_SRV002_LINK = "TRANFER_SALARY_T24_SRV002_LINK";
	public static final String TRANFER_SALARY_T24_SRV001_LINK = "TRANFER_SALARY_T24_SRV001_LINK";
	public static final String TRANFER_SALARY_T24_SRV039_LINK = "TRANFER_SALARY_T24_SRV039_LINK";
	public static final String TRANFER_SALARY_T24_SRV003_LINK = "TRANFER_SALARY_T24_SRV003_LINK";
	public static final String TRANFER_SALARY_T24_SRV112_LINK = "TRANFER_SALARY_T24_SRV112_LINK";
	public static final String TRANFER_SALARY_T24_SRV492_LINK = "TRANFER_SALARY_T24_SRV492_LINK";
	public static final String TRANFER_SALARY_T24_SRV942_LINK = "TRANFER_SALARY_T24_SRV942_LINK";

	public static final String TRANFER_SALARY_T24_TRANCODE_FEE = "TRANFER_SALARY_T24_TRANCODE_FEE";
	public static final String TRANFER_SALARY_T24_TRANCODE_FEE_GL = "TRANFER_SALARY_T24_TRANCODE_FEE_GL";
	public static final String TRANFER_SALARY_T24_ESB_TELLER_APPROVE = "TRANFER_SALARY_T24_ESB_TELLER_APPROVE";
	public static final String TRANFER_SALARY_T24_GL_ACCOUNT_SERVICE_FEE = "TRANFER_SALARY_T24_GL_ACCOUNT_SERVICE_FEE";
	public static final String TRANFER_SALARY_T24_GL_ACCOUNT_TRANFER_FEE = "TRANFER_SALARY_T24_GL_ACCOUNT_TRANFER_FEE";
	public static final String TRANFER_SALARY_T24_ESB_TELLER_APPROVE_GL = "TRANFER_SALARY_T24_ESB_TELLER_APPROVE_GL";
	
	public static final String TRANFER_ATM_T24 = "TRANFER_ATM_T24";
	public static final String TRANFER_SALARY_T24_TRANCODE_GL_CASA = "TRANFER_SALARY_T24_TRANCODE_GL_CASA";
	public static final String TRANFER_SALARY_BRANCH_CITAD = "TRANFER_SALARY_BRANCH_CITAD";
}
