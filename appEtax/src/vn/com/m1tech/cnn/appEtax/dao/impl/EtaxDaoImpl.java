package vn.com.m1tech.cnn.appEtax.dao.impl;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.springframework.stereotype.Repository;

import vn.com.m1tech.cnn.appEtax.common.Utils;
import vn.com.m1tech.cnn.appEtax.dao.EtaxDao;
import vn.com.m1tech.cnn.appEtax.entities.FileNameUploadXml;
import vn.com.m1tech.cnn.appEtax.entities.IdentifyInfo;
import vn.com.m1tech.cnn.appEtax.entities.PeriodUploadXml;
import vn.com.m1tech.cnn.appEtax.entities.TaxPersonInfo;
import vn.com.m1tech.cnn.dao.ApplicationContextHolder;
import vn.com.m1tech.cnn.dao.HibernateDao;
import vn.com.m1tech.cnn.domain.admin.Users;

import static org.hamcrest.CoreMatchers.equalTo;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Repository("etaxDao")
public class EtaxDaoImpl extends HibernateDao<FileNameUploadXml, Long> implements EtaxDao {

	@Override
	public Object countDupplicate(FileNameUploadXml search, Users user) {
		List<Object> listParam = new ArrayList<Object>();
		try {

			String hql = " FROM FileNameUploadXml a where 1= 1 ";

			if (!Utils.checkIsNullOrEmpty(search.getPeriod())) {
				hql = String.valueOf(hql) + " and a.period = ? ";
				listParam.add(search.getPeriod().trim());
			}

			if (!Utils.checkIsNullOrEmpty(search.getFileName())) {
				hql = String.valueOf(hql) + " and a.fileName = ? ";
				listParam.add(search.getFileName().trim());
			}
			
			if (!Utils.checkIsNullOrEmpty(search.getStatus()) && !"-1".equals(search.getStatus())) {
				hql = String.valueOf(hql) + " and a.status = ? ";
				listParam.add(search.getStatus().trim());
			}

			Query query = currentSession().createQuery("SELECT count(a.id) " + hql);
			for (int i = 0; i < listParam.size(); i++)
				query.setParameter(i, listParam.get(i));

			Long rows = ((Long) query.uniqueResult());
			return rows;

		} catch (Exception e) {
			e.printStackTrace();
		}
		return new ArrayList<FileNameUploadXml>();
	}

	@Override
	public Object countTotal(FileNameUploadXml search, Users user) {
		List<Object> listParam = new ArrayList<Object>();
		try {

			String hql = " FROM FileNameUploadXml a where 1= 1 ";

			if (!Utils.checkIsNullOrEmpty(search.getPeriod())) {
				hql = String.valueOf(hql) + " and a.period = ? ";
				listParam.add(search.getPeriod().trim());
			}

			if (!Utils.checkIsNullOrEmpty(search.getFileName())) {
				hql = String.valueOf(hql) + " and a.fileName like ? ";
				listParam.add("%" + search.getFileName().trim() + "%");
			}
			
			if (!Utils.checkIsNullOrEmpty(search.getStatus()) && !"-1".equals(search.getStatus())) {
				hql = String.valueOf(hql) + " and a.status = ? ";
				listParam.add(search.getStatus().trim());
			}

			hql = String.valueOf(hql) + " order by a.id  ";

			Query query = currentSession().createQuery("SELECT count(a.id) " + hql);
			for (int i = 0; i < listParam.size(); i++)
				query.setParameter(i, listParam.get(i));

			Long rows = ((Long) query.uniqueResult());
			return rows;

		} catch (Exception e) {
			e.printStackTrace();
		}
		return new ArrayList<FileNameUploadXml>();
	}

	@Override
	public List<FileNameUploadXml> getListXmlFileName(FileNameUploadXml search, int maxRecord) {
		List<Object> listParam = new ArrayList<Object>();
		List<FileNameUploadXml> lst = new ArrayList<FileNameUploadXml>();
		try {
			String hql = "SELECT id, period, fileName, createBy, createTime, numberOfRecords, status, description from FileNameUploadXml a where 1= 1 ";

			if (!Utils.checkIsNullOrEmpty(search.getPeriod())) {
				hql = String.valueOf(hql) + " and a.period = ? ";
				listParam.add(search.getPeriod().trim());
			}

			if (!Utils.checkIsNullOrEmpty(search.getFileName())) {
				hql = String.valueOf(hql) + " and a.fileName like ? ";
				listParam.add("%" + search.getFileName().trim() + "%");
			}

			if (!Utils.checkIsNullOrEmpty(search.getStatus()) && !"-1".equals(search.getStatus())) {
				hql = String.valueOf(hql) + " and a.status = ? ";
				listParam.add(search.getStatus().trim());
			}

			hql = String.valueOf(hql) + " order by a.id desc ";

			Query query = currentSession().createQuery(hql);

			final int startIdx = (search.getPage() - 1) * search.getRows();
			final int endIdx = Math.min(startIdx + search.getRows(), maxRecord);

			for (int i = 0; i < listParam.size(); i++)
				query.setParameter(i, listParam.get(i));

			query.setFirstResult(startIdx);
			query.setMaxResults(endIdx);

			List<Object[]> listResult = query.list();

			for (Object[] aRow : listResult) {
				FileNameUploadXml etax = new FileNameUploadXml();
				etax.setId((Long) aRow[0]);
			    etax.setPeriod((String) aRow[1]);
			    etax.setFileName((String) aRow[2]);
			    etax.setCreateBy((String) aRow[3]);
			    etax.setCreateTime((Date) aRow[4]);
			    etax.setNumberOfRecords((Integer) aRow[5]);
			    etax.setStatus((String) aRow[6]);
			    etax.setDescription((String) aRow[7]);

				lst.add(etax);
			}
			return lst;

		} catch (Exception e) {
			e.printStackTrace();
		}
		return new ArrayList<FileNameUploadXml>();
	}

	@Override
	public boolean saveList(List<FileNameUploadXml> lstLog, List<PeriodUploadXml> lstPeriod) {
		SessionFactory sessionFactory = (SessionFactory) ApplicationContextHolder.getContext()
				.getBean("sessionFactory");
		Session session = sessionFactory.openSession();
		Transaction tx = null;
		try {
			tx = session.beginTransaction();
			for (int i = 0; i < lstLog.size(); i++) {
				FileNameUploadXml fileName = lstLog.get(i);
				if (fileName.getId() == null) {
		            session.persist(fileName); // For new entities
		        } else {
		            session.merge(fileName);   // For existing entities
		        }
				if (i > 0 && i % 1000 == 0) { // 20, same as the JDBC batch size
					// flush a batch of inserts and release memory:
					session.flush();
					session.clear();
				}
			}
			for (int i = 0; i < lstPeriod.size(); i++) {
				PeriodUploadXml period = lstPeriod.get(i);
				if (period.getId() == null) {
		            session.persist(period); // For new entities
		        } else {
		            session.merge(period);   // For existing entities
		        }
				if (i > 0 && i % 1000 == 0) { // 20, same as the JDBC batch size
					// flush a batch of inserts and release memory:
					session.flush();
					session.clear();
				}
			}
			tx.commit();
		} catch (Exception ex) {
			if (tx != null)
				tx.rollback();
			ex.printStackTrace();
			return false;
		} finally {
			session.close();
		}

		return true;
	}
	
	@Override
	public FileNameUploadXml getById(Long id) {
		try{
			String hql ="FROM FileNameUploadXml WHERE id =:id";
			Query query = currentSession().createQuery(hql);
			query.setParameter("id", id);
			return (FileNameUploadXml) query.uniqueResult();
		}catch(Exception e){
			e.printStackTrace();
		}
		return null;
	}
	
	@Override
	public List<FileNameUploadXml> getFileNamesByPeriod(FileNameUploadXml search, int maxRecord) {
		List<Object> listParam = new ArrayList<Object>();
		List<FileNameUploadXml> fileNames = new ArrayList<FileNameUploadXml>();
	    try {
	        String hql = "FROM FileNameUploadXml a where 1= 1 and a.period = ?";
	        Query query = currentSession().createQuery(hql);

			final int startIdx = (search.getPage() - 1) * search.getRows();
			final int endIdx = Math.min(startIdx + search.getRows(), maxRecord);

			for (int i = 0; i < listParam.size(); i++)
				query.setParameter(i, listParam.get(i));

			query.setFirstResult(startIdx);
			query.setMaxResults(endIdx);

			fileNames = query.list();
			return fileNames;
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	    return fileNames;
	}
	
	@Override
	public PeriodUploadXml getByPeriod(String period) {
		try{
			String hql ="FROM PeriodUploadXml WHERE period =:period";
			Query query = currentSession().createQuery(hql);
			query.setParameter("period", period);
			return (PeriodUploadXml) query.uniqueResult();
		}catch(Exception e){
			e.printStackTrace();
		}
		return null;
	}
	
	@Override
	public List<TaxPersonInfo> getTaxPersonInfo(TaxPersonInfo search, int maxRecord) {
		List<Object> listParam = new ArrayList<Object>();
		List<TaxPersonInfo> lst = new ArrayList<TaxPersonInfo>();
		try {

			String hql = " from TaxPersonInfo a where 1= 1 ";

			if (!Utils.checkIsNullOrEmpty(search.getMst())) {
				hql = String.valueOf(hql) + " and a.mst = ? ";
				listParam.add(search.getMst().trim());
			}
			
			if (!Utils.checkIsNullOrEmpty(search.getFileName())) {
				hql = String.valueOf(hql) + " and a.fileName = ? ";
				listParam.add(search.getFileName().trim());
			}
			
			if (!Utils.checkIsNullOrEmpty(search.getTenNnt())) {
				hql = String.valueOf(hql) + " and a.tenNnt = ? ";
				listParam.add(search.getTenNnt().trim());
			}
			
			if (search.getCreatedDate() != null) {
				hql = String.valueOf(hql) + " and a.createdDate = ? ";
				listParam.add(search.getCreatedDate());
			}
			
			if (!Utils.checkIsNullOrEmpty(search.getpBanTLieuXml())) {
				hql = String.valueOf(hql) + " and a.pBanTLieuXml = ? ";
				listParam.add(search.getpBanTLieuXml());
			}

			hql = String.valueOf(hql) + " order by a.id desc ";

			Query query = currentSession().createQuery(hql);

			for (int i = 0; i < listParam.size(); i++)
				query.setParameter(i, listParam.get(i));

			lst = query.list();
			return lst;

		} catch (Exception e) {
			e.printStackTrace();
		}
		return new ArrayList<TaxPersonInfo>();
	}
	
	@Override
	public List<IdentifyInfo> getIdentifyInfo(IdentifyInfo search, int maxRecord) {
		List<Object> listParam = new ArrayList<Object>();
		List<IdentifyInfo> lst = new ArrayList<IdentifyInfo>();
		try {

			String hql = " from IdentifyInfo a where 1= 1 ";

			if (!Utils.checkIsNullOrEmpty(search.getMst())) {
				hql = String.valueOf(hql) + " and a.mst = ? ";
				listParam.add(search.getMst().trim());
			}
			
			if (!Utils.checkIsNullOrEmpty(search.getFileName())) {
				hql = String.valueOf(hql) + " and a.fileName = ? ";
				listParam.add(search.getFileName().trim());
			}
			
			if (!Utils.checkIsNullOrEmpty(search.getSoGiayTo())) {
				hql = String.valueOf(hql) + " and a.soGiayTo = ? ";
				listParam.add(search.getSoGiayTo().trim());
			}
			
			if (!Utils.checkIsNullOrEmpty(search.getLoaiGiayTo())) {
				hql = String.valueOf(hql) + " and a.loaiGiayTo = ? ";
				listParam.add(search.getLoaiGiayTo());
			}

			hql = String.valueOf(hql) + " order by a.id desc ";

			Query query = currentSession().createQuery(hql);

			for (int i = 0; i < listParam.size(); i++)
				query.setParameter(i, listParam.get(i));

			lst = query.list();
			return lst;

		} catch (Exception e) {
			e.printStackTrace();
		}
		return new ArrayList<IdentifyInfo>();
	}
}
