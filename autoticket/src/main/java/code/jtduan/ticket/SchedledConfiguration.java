package code.jtduan.ticket;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.CronTriggerFactoryBean;
import org.springframework.scheduling.quartz.MethodInvokingJobDetailFactoryBean;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

/**
 * @author jtduan
 * @date 2016/12/21
 */
@Configuration
public class SchedledConfiguration {

    @Autowired
    public ApplicationContext ctx;

    @Value("${cron}")
    public String cron;

    @Bean(name = "order")
    public MethodInvokingJobDetailFactoryBean detailFactoryBean() {
        MethodInvokingJobDetailFactoryBean bean = new MethodInvokingJobDetailFactoryBean();
        bean.setTargetObject(ctx.getBean(TService.class));
        bean.setTargetMethod("run");
        bean.setConcurrent(false);
        bean.setName("order");
        return bean;
    }

    @Bean(name = "ordertrigger")
    public CronTriggerFactoryBean myConf() {
        CronTriggerFactoryBean bean = new CronTriggerFactoryBean();
        bean.setCronExpression(cron);
        bean.setName("ordertrigger");
        bean.setJobDetail(detailFactoryBean().getObject());
        return bean;
    }

    @Bean(name = "sessionjob")
    public MethodInvokingJobDetailFactoryBean sessionFactoryBean() {
        MethodInvokingJobDetailFactoryBean bean = new MethodInvokingJobDetailFactoryBean();
        bean.setTargetObject(ctx.getBean(TService.class));
        bean.setTargetMethod("keepSession");
        bean.setConcurrent(false);
        bean.setName("sessionjob");
        return bean;
    }

    @Bean(name = "sessiontrigger")
    public CronTriggerFactoryBean sessionConf() {
        CronTriggerFactoryBean bean = new CronTriggerFactoryBean();
        bean.setCronExpression("0 2/3 * * * ?");
        bean.setName("sessiontrigger");
        bean.setJobDetail(sessionFactoryBean().getObject());
        return bean;
    }

    @Bean
    public SchedulerFactoryBean schedulerFactory() {
        SchedulerFactoryBean schedulerFactory = new SchedulerFactoryBean();
        schedulerFactory.setTriggers(myConf().getObject(), sessionConf().getObject());
        schedulerFactory.setJobDetails(detailFactoryBean().getObject(), sessionFactoryBean().getObject());
        return schedulerFactory;
    }
}
