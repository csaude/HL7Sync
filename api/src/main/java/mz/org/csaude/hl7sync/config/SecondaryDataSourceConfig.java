package mz.org.csaude.hl7sync.config;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.jdbc.core.JdbcTemplate;


import javax.sql.DataSource;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
        basePackages = "mz.org.csaude.hl7sync.dao.hl7filegenerator",
        entityManagerFactoryRef = "openmrsEntityManagerFactory",
        transactionManagerRef = "openmrsTransactionManager")
public class SecondaryDataSourceConfig {

    @Bean
    @ConfigurationProperties(prefix = "openmrs.datasource")
    public DataSource openmrsDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean openmrsEntityManagerFactory(EntityManagerFactoryBuilder builder,
                                                                              @Qualifier("openmrsDataSource") DataSource openmrsDataSource) {
        return builder
                .dataSource(openmrsDataSource)
                .packages("mz.org.csaude.hl7sync.model")
                .persistenceUnit("openmrs")
                .build();
    }

    @Bean
    public PlatformTransactionManager openmrsTransactionManager(@Qualifier("openmrsEntityManagerFactory") EntityManagerFactory openmrsEntityManagerFactory) {
        return new JpaTransactionManager(openmrsEntityManagerFactory);
    }

    @Bean
    public JdbcTemplate openmrsJdbcTemplate(@Qualifier("openmrsDataSource") DataSource openmrsDataSource) {
        return new JdbcTemplate(openmrsDataSource);
    }
}