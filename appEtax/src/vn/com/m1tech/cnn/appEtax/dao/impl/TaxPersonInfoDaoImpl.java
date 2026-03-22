package vn.com.m1tech.cnn.appEtax.dao.impl;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.springframework.stereotype.Repository;
import vn.com.m1tech.cnn.appEtax.dao.TaxPersonInfoDao;
import vn.com.m1tech.cnn.appEtax.entities.TaxPersonInfo;
import vn.com.m1tech.cnn.dao.ApplicationContextHolder;
import vn.com.m1tech.cnn.dao.HibernateDao;

@Repository("taxPersonInfoDao")
public class TaxPersonInfoDaoImpl extends HibernateDao<TaxPersonInfo, Long> implements TaxPersonInfoDao {

	@Override
	public boolean saveList(List<TaxPersonInfo> lstLog) {
		SessionFactory sessionFactory = (SessionFactory) ApplicationContextHolder.getContext()
				.getBean("sessionFactory");
		Session session = sessionFactory.openSession();
		Transaction tx = null;

		try {

			tx = session.beginTransaction();

			for (int i = 0; i < lstLog.size(); i++) {

				// session.persist(lstLog.get(i));
				if (i > 0 && i % 1000 == 0) { // 20, same as the JDBC batch size
					// flush a batch of inserts and release memory:
					session.flush();
					session.clear();

					// em.flush();
					// em.clear();
				}

				session.save(lstLog.get(i));

				// session.save(lstLog.get(i));
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
