package com.chronos.monitoring;

import com.chronos.domain.model.Job;
import com.chronos.domain.model.enums.JobStatus;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Aspect
@Component
public class MetricsAspect {

    private final AtomicInteger runningJobs = new AtomicInteger(0);
    private final Counter jobSubmissionsTotal;
    private final Counter jobRetriesTotal;
    private final MeterRegistry registry;

    public MetricsAspect(MeterRegistry registry) {
        this.registry = registry;
        this.jobSubmissionsTotal = Counter.builder("chronos_jobs_submissions_total")
            .description("Total number of job submissions")
            .register(registry);
        
        this.jobRetriesTotal = Counter.builder("chronos_jobs_retries_total")
            .description("Total number of job retries")
            .register(registry);
        
        // Register the running jobs gauge
        Gauge.builder("chronos_jobs_running", runningJobs, AtomicInteger::get)
            .description("Number of currently running jobs")
            .register(registry);
    }

    @Around("@annotation(io.micrometer.core.annotation.Timed)")
    public Object timeMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        Timer.Sample sample = Timer.start(registry);
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        
        try {
            return joinPoint.proceed();
        } finally {
            sample.stop(Timer.builder("chronos.method.execution")
                    .tag("class", className)
                    .tag("method", methodName)
                    .description("Time taken to execute method")
                    .publishPercentiles(0.5, 0.95, 0.99)
                    .register(registry));
        }
    }

    @Around("execution(* com.chronos.service.impl.JobServiceImpl.createJob(..))")
    public Object trackJobCreation(ProceedingJoinPoint joinPoint) throws Throwable {
        Timer.Sample sample = Timer.start(registry);
        
        try {
            Object result = joinPoint.proceed();
            jobSubmissionsTotal.increment();
            return result;
        } catch (Exception e) {
            registry.counter("chronos.jobs.failures", "operation", "create")
                    .increment();
            throw e;
        } finally {
            sample.stop(Timer.builder("chronos.jobs.creation.duration")
                    .register(registry));
        }
    }

    @Around("execution(* com.chronos.service.impl.JobServiceImpl.triggerJobNow(..))")
    public Object trackJobTrigger(ProceedingJoinPoint joinPoint) throws Throwable {
        Timer.Sample sample = Timer.start(registry);
        
        try {
            Object result = joinPoint.proceed();
            registry.counter("chronos.jobs.submissions", "type", "trigger")
                    .increment();
            return result;
        } catch (Exception e) {
            registry.counter("chronos.jobs.failures", "operation", "trigger")
                    .increment();
            throw e;
        } finally {
            sample.stop(Timer.builder("chronos.jobs.trigger.duration")
                    .register(registry));
        }
    }

    @Around("execution(* com.chronos.service.impl.JobExecutorServiceImpl.execute*(..))")    public Object trackJobExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        String jobType = joinPoint.getSignature().getName().replace("execute", "").toLowerCase();
        Timer.Sample sample = Timer.start(registry);
        runningJobs.incrementAndGet();
        
        try {
            Object result = joinPoint.proceed();
            registry.counter("chronos.jobs.executions", "type", jobType, "outcome", "success")
                    .increment();
            return result;
        } catch (Exception e) {
            registry.counter("chronos.jobs.executions", "type", jobType, "outcome", "failure")
                    .increment();
            throw e;
        } finally {
            runningJobs.decrementAndGet();
            sample.stop(Timer.builder("chronos.jobs.execution.duration")
                    .tag("type", jobType)
                    .register(registry));
        }
    }

    @Around("execution(* com.chronos.service.impl.QuartzSchedulerServiceImpl.rescheduleJob(..))")
    public Object trackJobRetry(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            Object[] args = joinPoint.getArgs();
            if (args.length >= 3 && args[0] instanceof Job && args[2] instanceof Integer) {
                Job job = (Job) args[0];
                int attempt = (Integer) args[2];
                if (attempt > 1) {
                    jobRetriesTotal.increment();
                    registry.counter("chronos.jobs.retries", 
                            "type", job.getType().name().toLowerCase(),
                            "status", "attempted")
                            .increment();
                }
            }
            return joinPoint.proceed();
        } catch (Exception e) {
            throw e;
        }
    }

    @After("execution(* com.chronos.service.impl.JobServiceImpl.updateJobStatus(..)) && args(job, status)")
    public void trackJobStatus(Job job, JobStatus status) {
        registry.counter("chronos.jobs.status", "status", status.name())
                .increment();
    }
}
