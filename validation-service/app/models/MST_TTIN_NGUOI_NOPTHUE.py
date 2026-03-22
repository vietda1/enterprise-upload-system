from sqlalchemy import Column, Integer, String, DateTime, Text, JSON, BigInteger
from datetime import datetime
from app.database import Base


class TaxModels(Base):
    """Tax Information Model"""
    
    __tablename__ = "MST_TTIN_NGUOI_NOPTHUE"
    
    id = Column(Integer, index=True)
    mst = Column(String(100), index=True, nullable=False) # Mã số thuế
    ten_nnt = Column(String(200))  # Tên người nộp thuế
    ngay_sinh = Column(String(100)) # Ngày sinh
    loai_nnt = Column(String(100)) # Loại người nộp thuế (EB/RB)
    tthai_mst = Column(String(100)) # Trạng thái mã số thuế
    pban_tlieu_xml = Column(String(100))
    
    # Thông tin file
    thang_gui = Column(String(100)) # Tháng gửi
    file_name = Column(String(100)) # Tên file chứa thông tin người nộp thuế
    
    # Thông tin import
    created_by = Column(String(100)) # Người import
    created_date = Column(DateTime, default=datetime.utcnow, nullable=False)
    id_person_info = Column(String(100))
    
    def __repr__(self):
        return f"<TaxModels(mst={self.mst}, ten_nnt={self.ten_nnt})>"
    
class TtinGiaytoModels(Base):
    """ID Card Information Model"""
    
    __tablename__ = "MST_TTIN_GIAYTO"
    
    id = Column(Integer, index=True)
    so_giayto = Column(String(100), index=True, nullable=False) # Mã số giấy tờ
    loai_giayto = Column(String(200))  # Loại giấy tờ
    ten_loai_giayto = Column(String(100)) # Tên loại giấy tờ
    mst = Column(String(100)) # Mã số thuế

    # Thông tin gửi file
    thang_gui = Column(String(100)) # Tháng gửi
    ngay_gui = Column(String(100)) # Ngày gửi
    lan_gui = Column(String(100)) # Lần gửi
    file_name = Column(String(100)) # Tên file chứa thông tin người nộp thuế
    
    # Thông tin import
    id_person_info = Column(String(100)) # Người import
        
    def __repr__(self):
        return f"<TtinGiaytoModels(so_giayto={self.so_giayto}, ten_loai_giayto={self.ten_loai_giayto})>"