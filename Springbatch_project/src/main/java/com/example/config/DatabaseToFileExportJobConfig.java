package com.example.config;


import com.example.Processor.PersonItemProcessor;
import com.example.Writer.ExcelItemWriter;
import com.example.entity.Person;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.Order;
import org.springframework.batch.item.database.PagingQueryProvider;
import org.springframework.batch.item.database.support.SqlPagingQueryProviderFactoryBean;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.transform.BeanWrapperFieldExtractor;
import org.springframework.batch.item.file.transform.DelimitedLineAggregator;
import org.springframework.batch.item.support.CompositeItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class DatabaseToFileExportJobConfig {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private DataSource dataSource;

    @Bean
    public JdbcPagingItemReader<Person> databaseReader() throws Exception {
        JdbcPagingItemReader<Person> reader = new JdbcPagingItemReader<>();
        reader.setDataSource(dataSource);
        reader.setPageSize(100);
        reader.setRowMapper(new BeanPropertyRowMapper<>(Person.class));

        SqlPagingQueryProviderFactoryBean queryProvider = new SqlPagingQueryProviderFactoryBean();
        queryProvider.setDataSource(dataSource);
        queryProvider.setSelectClause("SELECT id, first_name, last_name, email, birth_date");
        queryProvider.setFromClause("FROM persons");

        Map<String, Order> sortKeys = new HashMap<>();
        sortKeys.put("id", Order.ASCENDING);
        queryProvider.setSortKeys(sortKeys);

        reader.setQueryProvider(queryProvider.getObject());

        return reader;
    }

    @Bean
    public PersonItemProcessor processor() {
        return new PersonItemProcessor();
    }

    @Bean
    public FlatFileItemWriter<Person> csvItemWriter() {
        FlatFileItemWriter<Person> writer = new FlatFileItemWriter<>();
        writer.setResource(new FileSystemResource("output/persons.csv"));
        writer.setAppendAllowed(false);

        DelimitedLineAggregator<Person> lineAggregator = new DelimitedLineAggregator<>();
        lineAggregator.setDelimiter(",");

        BeanWrapperFieldExtractor<Person> fieldExtractor = new BeanWrapperFieldExtractor<>();
        fieldExtractor.setNames(new String[] {"id", "firstName", "lastName", "email", "birthDate"});
        lineAggregator.setFieldExtractor(fieldExtractor);

        writer.setLineAggregator(lineAggregator);
        writer.setHeaderCallback(writer1 -> writer1.write("ID,First Name,Last Name,Email,Birth Date"));

        return writer;
    }

    @Bean
    public ExcelItemWriter<Person> excelItemWriter() {
        ExcelItemWriter<Person> writer = new ExcelItemWriter<>();
        writer.setResource(new FileSystemResource("output/persons.xlsx"));
        writer.setSheetName("Persons");
        writer.setColumnNames(new String[] {"ID", "First Name", "Last Name", "Email", "Birth Date"});
        writer.setPropertyNames(new String[] {"id", "firstName", "lastName", "email", "birthDate"});
        return writer;
    }

    @Bean
    public CompositeItemWriter<Person> compositeItemWriter() {
        CompositeItemWriter<Person> writer = new CompositeItemWriter<>();
        writer.setDelegates(Arrays.asList(csvItemWriter(), excelItemWriter()));
        return writer;
    }

    @Bean
    public Step exportStep() throws Exception {
        return new StepBuilder("exportStep", jobRepository)
                .<Person, Person>chunk(10, transactionManager)
                .reader(databaseReader())
                .processor(processor())
                .writer(compositeItemWriter())
                .build();
    }

    @Bean
    public Job exportJob() throws Exception {
        return new JobBuilder("exportJob", jobRepository)
                .start(exportStep())
                .build();
    }
}