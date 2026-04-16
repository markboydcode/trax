package nbdp.trax.cfg;

import java.util.Map;

import javax.sql.DataSource;

import nbdp.trax.ServiceLocator;
import nbdp.trax.data.I_TraxDao;
import nbdp.trax.data.ibatis.TraxDao;
import nbdp.trax.report.ReportEngine;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

@Configuration
public class TraxConfig
{
    @Bean
    public Holder configHolder()
    {
        Holder holder = new Holder();
        holder.setConfig(Map.of("window.classpath.image.file", "images/alarmClock.png"));
        return holder;
    }

    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception
    {
        SqlSessionFactoryBean factory = new SqlSessionFactoryBean();
        factory.setDataSource(dataSource);
        factory.setMapperLocations(new ClassPathResource("mybatis-traxDao.xml"));
        return factory.getObject();
    }

    @Bean
    public TraxDao traxDao(SqlSessionFactory sqlSessionFactory)
    {
        TraxDao dao = new TraxDao();
        dao.setSqlSessionFactory(sqlSessionFactory);
        return dao;
    }

    @Bean
    public ReportEngine reportEngine(I_TraxDao dao)
    {
        return new ReportEngine(dao);
    }

    @Bean
    public ServiceLocator serviceLocator(I_TraxDao dao)
    {
        ServiceLocator locator = ServiceLocator.getInstance();
        locator.setDAO(dao);
        return locator;
    }
}