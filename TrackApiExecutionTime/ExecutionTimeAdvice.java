
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

// Custom TrackExecutionTime annotation definition file
//that measures time taken to execute the method/function on adding annotation over the method/function
//and logs to console and log file
@Aspect
@Component
// @ConditionalOnExpression("${aspect.enabled:true}")
public class ExecutionTimeAdvice {

    Logger log = LoggerFactory.getLogger(ExecutionTimeAdvice.class);
	
    @Around("@annotation(TrackExecutionTime)")
    public Object executionTime(ProceedingJoinPoint point) throws Throwable {
        log.info("Inside Method: "+ point.getSignature().getName());
        long startTime = System.currentTimeMillis();
        Object object = point.proceed();
        long endtime = System.currentTimeMillis();
        log.info("Class Name: "+ point.getSignature().getDeclaringTypeName() +". Method Name: "+ point.getSignature().getName() + ". Time taken for Execution is : " + (endtime-startTime) +"ms");
//        log.info("Class Name: "+ point.getSignature().getDeclaringTypeName() +". Method Name: "+ point.getSignature().getName() + ". Time taken for Execution is : " + (endtime-startTime) +"ms");

        return object;
    }
}

