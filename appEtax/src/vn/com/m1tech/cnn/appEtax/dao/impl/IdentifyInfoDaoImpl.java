package vn.com.m1tech.cnn.appEtax.dao.impl;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.springframework.stereotype.Repository;

import vn.com.m1tech.cnn.appEtax.dao.IdentifyInfoDao;
import vn.com.m1tech.cnn.appEtax.entities.IdentifyInfo;
import vn.com.m1tech.cnn.dao.ApplicationContextHolder;
import vn.com.m1tech.cnn.dao.HibernateDao;

@Repository("identifyInfoDao")
public class IdentifyInfoDaoImpl extends HibernateDao<IdentifyInfo, Long> implements IdentifyInfoDao {

	@Override
	public boolean saveList(List<IdentifyInfo> lstLog) {
		SessionFactory sessionFactory = (SessionFactory) ApplicationContextHolder.getContext()
				.getBean("sessionFactory");
		Session session = sessionFactory.openSession();
		Transaction tx = null;
		try {
			tx = session.beginTransaction();
			for (int i = 0; i < lstLog.size(); i++) {
				session.persist(lstLog.get(i));
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
}
