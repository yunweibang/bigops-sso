package com.yunweibang.auth.utils;

import com.alibaba.druid.pool.DruidDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

@SuppressWarnings("unused")
public class JdbcUtils {
    private static final Logger logger = LoggerFactory.getLogger(JdbcUtils.class);

    private static DruidDataSource ds = new DruidDataSource();
    private static String userName;
    private static String passWord;
    private static String dataSourceUrl;
    private static String domainUrl;

    static {
        Properties prop = new Properties();
        String url;
        String username;
        String password;
        String driverClassName = "com.mysql.jdbc.Driver";
        Integer initialSize;
        Integer minIdle;
        Integer maxActive;
        Integer maxWait;
        Integer maxPoolPreparedStatementPerConnectionSize;
        String validationQuery;
        Integer maxOpenPreparedStatements;
        Integer validationQueryTimeout;
        Integer timeBetweenEvictionRunsMillis = 60000;
        Integer minEvictableIdleTimeMillis = 300000;
        Boolean testWhileIdle = true;
        Boolean testOnBorrow = false;
        Boolean testOnReturn = false;
        Boolean poolPreparedStatements = true;
        String filters = "stat";

        try {
            // //prop.load(in);
            prop.load(new FileReader("/opt/bigops/config/bigops.properties"));
            url = prop.getProperty("spring.datasource.url").trim();
            username = prop.getProperty("spring.datasource.username").trim();
            password = prop.getProperty("spring.datasource.password").trim();
            initialSize = Integer.parseInt(prop.getProperty("spring.datasource.druid.initial-size").trim());
            maxActive = Integer.parseInt(prop.getProperty("spring.datasource.druid.max-active").trim());
            minIdle = Integer.parseInt(prop.getProperty("spring.datasource.druid.min-idle").trim());
            maxWait = Integer.parseInt(prop.getProperty("spring.datasource.druid.max-wait").trim());
            maxPoolPreparedStatementPerConnectionSize = Integer.parseInt(
                    prop.getProperty("spring.datasource.druid.max-pool-prepared-statement-per-connection-size").trim());
            maxOpenPreparedStatements = Integer
                    .parseInt(prop.getProperty("spring.datasource.druid.max-open-prepared-statements").trim());
            validationQuery = prop.getProperty("spring.datasource.druid.validation-query").trim();
            validationQueryTimeout = Integer
                    .parseInt(prop.getProperty("spring.datasource.druid.validation-query-timeout").trim());
            setUserName(username);
            setPassWord(password);
            setDataSourceUrl(url);
            domainUrl = prop.getProperty("home.url").trim();

        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("解析 /opt/bigops/config/bigops.properties 异常,请检查配置文件");
        }
        logger.info("bigops.properties 加载成功");
        ds.setUsername(username);
        ds.setPassword(password);
        ds.setUrl(url);
        ds.setDriverClassName(driverClassName);
        ds.setInitialSize(initialSize); // 定义初始连接数
        ds.setMinIdle(minIdle); // 最小空闲
        ds.setMaxActive(maxActive); // 定义最大连接数
        ds.setMaxWait(maxWait); // 最长等待时间

        // 配置间隔多久才进行一次检测，检测需要关闭的空闲连接，单位是毫秒
        ds.setTimeBetweenEvictionRunsMillis(timeBetweenEvictionRunsMillis);

        // 配置一个连接在池中最小生存的时间，单位是毫秒
        ds.setMinEvictableIdleTimeMillis(minEvictableIdleTimeMillis);
        ds.setValidationQuery(validationQuery);
        ds.setTestWhileIdle(testWhileIdle);
        ds.setTestOnBorrow(testOnBorrow);
        ds.setTestOnReturn(testOnReturn);
        ds.setValidationQueryTimeout(validationQueryTimeout);
        // 打开PSCache，并且指定每个连接上PSCache的大小
        ds.setPoolPreparedStatements(poolPreparedStatements);
        ds.setMaxPoolPreparedStatementPerConnectionSize(maxPoolPreparedStatementPerConnectionSize);

        ds.setMaxOpenPreparedStatements(maxOpenPreparedStatements);
        try {
            ds.setFilters(filters);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static String getDomainUrl() {
        return domainUrl;
    }

    public static String getDataSourceUrl() {
        return dataSourceUrl;
    }

    public static void setDataSourceUrl(String dataSourceUrl) {
        JdbcUtils.dataSourceUrl = dataSourceUrl;
    }

    public static String getUserName() {
        return userName;
    }

    public static void setUserName(String userName) {
        JdbcUtils.userName = userName;
    }

    public static String getPassWord() {
        return passWord;
    }

    public static void setPassWord(String passWord) {
        JdbcUtils.passWord = passWord;
    }

    /**
     * 它为null表示没有事务 它不为null表示有事务 当开启事务时，需要给它赋值 当结束事务时，需要给它赋值为null
     * 并且在开启事务时，让dao的多个方法共享这个Connection
     */
    private static ThreadLocal<Connection> tl = new ThreadLocal<>();

    public static DataSource getDataSource() {

        return ds;
    }
    //
    // private static void setDataSource(DataSource ds) {
    // JdbcUtils.ds = ds;
    // }

    public static DruidDataSource getDs() {
        // setDs(ds);
        return ds;
    }

    public static void setDs(DruidDataSource ds) {
        JdbcUtils.ds = ds;
    }

    /**
     * 获取当前线程数据库连接
     *
     * @return Connection
     * @throws SQLException
     */
    public static Connection getConnection() throws SQLException {
        // 如果有事务，返回当前事务的con
        // 如果没有事务，通过连接池返回新的con
        Connection con = tl.get();
        if (con != null) {
            return con;
        }
        return ds.getConnection();
    }

    /**
     * 开启事务
     *
     * @throws SQLException
     */
    public static void begin() throws SQLException {
        // 获取当前线程的事务连接
        Connection con = tl.get();
        if (con != null) {
            throw new SQLException("已经开启了事务，不能重复开启！");
        }
        // 给con赋值，表示开启了事务
        con = ds.getConnection();
        // 设置为手动提交
        con.setAutoCommit(false);
        tl.set(con);
    }

    /**
     * 提交事务
     *
     * @throws SQLException
     */
    public static void commit() throws SQLException {
        // 获取当前线程的事务连接
        Connection con = tl.get();
        if (con == null) {
            throw new SQLException("没有事务不能提交！");
        }
        con.commit();
        con.close();
        tl.remove();
    }

    /**
     * 回滚事务
     *
     * @throws SQLException
     */
    public static void rollback() throws SQLException {
        Connection con = tl.get();
        // 获取当前线程的事务连接
        if (con == null) {
            throw new SQLException("没有事务不能回滚！");
        }
        con.rollback();
        con.close();
        tl.remove();
    }

    /**
     * 释放Connection
     *
     * @param connection
     * @throws SQLException
     */
    public static void releaseConnection(Connection connection) throws SQLException {
        // 获取当前线程的事务连接
        Connection con = tl.get();
        // 如果参数连接，与当前事务连接不同，说明这个连接不是当前事务，可以关闭！
        if (connection != con) {
            // 如果参数连接没有关闭，关闭之！
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        }
    }
}
