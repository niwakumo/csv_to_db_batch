package com.example.ex.config;

import java.nio.charset.StandardCharsets;

import javax.sql.DataSource;
import javax.xml.crypto.Data;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.transaction.PlatformTransactionManager;

import com.example.ex.model.Employee;

@Configuration
public class SpringConfig {
    
    private final JobLauncher jobLauncher;
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    // 読み込むCSVファイルの実体
    @Value("${csv.path}")
    private Resource inputCSV;

    // データソースの設定(データベースの接続情報)
    @Autowired
    private DataSource dataSource;

    // SQL文の設定
    private static final String INSERT_EMP_SQL = """
        INSERT INTO employee (empnumber, empname, jobtitle, mgrnumber, hiredate)
        VALUES(:empNumber, :empName, :jobTitle, :mgrNumber, :hireDate)    
        """;

    // コンストラクタ
    public SpringConfig(JobLauncher jobLauncher, JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        this.jobLauncher = jobLauncher;
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
    }

    // ItemReaderの設定
    @Bean
    @StepScope
    public FlatFileItemReader<Employee> csvItemReader() {

        // BeanWrapperFieldSetMapperの設定
        BeanWrapperFieldSetMapper<Employee> fieldSetMapper = new BeanWrapperFieldSetMapper<>();
        fieldSetMapper.setTargetType(Employee.class);

        // CSVのカラム名を指定
        DelimitedLineTokenizer lineTokenizer = new DelimitedLineTokenizer();
        String [] csvTitleArray = new String[] {"empNumber", "empName", "jobTitle", "mgrNumber", "hireDate"};
        lineTokenizer.setNames(csvTitleArray);

        // LineMapperの設定
        DefaultLineMapper<Employee> lineMapper = new DefaultLineMapper<>();
        lineMapper.setFieldSetMapper(fieldSetMapper);
        lineMapper.setLineTokenizer(lineTokenizer);

        FlatFileItemReader<Employee> reader = new FlatFileItemReader<>();
        reader.setResource(inputCSV); // 読み込むCSVソースの指定
        reader.setLinesToSkip(1); // 1行目はヘッダーなのでスキップ
        reader.setEncoding(StandardCharsets.UTF_8.name()); // 文字コードの指定
        reader.setLineMapper(lineMapper); // マッピング処理の指定

        return reader;
    }

    // ItemProcessorの設定
    @Autowired
    @Qualifier("EmpItemProcessor")
    public ItemProcessor<Employee, Employee> empItemProcessor;

    // ItemWriterの設定
    @Bean
    @StepScope
    public JdbcBatchItemWriter<Employee> jdbcBatchItemWriter() {

        // providerをEmployeeクラスに指定
        BeanPropertyItemSqlParameterSourceProvider<Employee> provider = new BeanPropertyItemSqlParameterSourceProvider<>();

        // writerの設定
        JdbcBatchItemWriter<Employee> writer = new JdbcBatchItemWriter<>();
        writer.setDataSource(dataSource); // データソースの指定
        writer.setItemSqlParameterSourceProvider(provider); // providerのパラメータの指定
        writer.setSql(INSERT_EMP_SQL); // SQL文の指定

        return writer;
    }

    // Stepの設定
    @Bean
    public Step chunkStep1() {
        return new StepBuilder("EmpImportStep1", jobRepository)
            .<Employee, Employee>chunk(1, transactionManager) // 1件ずつ処理
            .reader(csvItemReader()) // ItemReaderの指定
            .processor(empItemProcessor) // ItemProcessorの指定
            .writer(jdbcBatchItemWriter()) // ItemWriterの指定
            .build();
        
    }

    // Jobの設定
    @Bean
    public Job chunkJob() {
        return new JobBuilder("chunkJob", jobRepository)
            .incrementer(new RunIdIncrementer()) // IDのインクリメント
            .start(chunkStep1()) // Stepの指定
            .build();

    }

}
