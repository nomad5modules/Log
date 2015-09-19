package hugo.weaving.internal;

import android.os.Looper;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.CodeSignature;
import org.aspectj.lang.reflect.MethodSignature;

import java.util.concurrent.TimeUnit;

import momentum.lib.log.Log;

@Aspect
public class Hugo
{
    @Pointcut("within(@DebugLog *)")
    public void withinAnnotatedClass()
    {
    }

    @Pointcut("execution(* *(..)) && withinAnnotatedClass()")
    public void methodInsideAnnotatedType()
    {
    }

    @Pointcut("execution(*.new(..)) && withinAnnotatedClass()")
    public void constructorInsideAnnotatedType()
    {
    }

    @Pointcut("execution(@DebugLog * *(..)) || methodInsideAnnotatedType()")
    public void method()
    {
    }

    @Pointcut("execution(@DebugLog *.new(..)) || constructorInsideAnnotatedType()")
    public void constructor()
    {
    }

    @Around("method() || constructor()")
    public Object logAndExecute(ProceedingJoinPoint joinPoint) throws Throwable
    {
        enterMethod(joinPoint);

        long startNanos = System.nanoTime();
        Object result = joinPoint.proceed();
        long stopNanos = System.nanoTime();
        long lengthMillis = TimeUnit.NANOSECONDS.toMillis(stopNanos - startNanos);

        exitMethod(joinPoint, result, lengthMillis);

        return result;
    }

    private static void enterMethod(JoinPoint joinPoint)
    {
        CodeSignature codeSignature = (CodeSignature) joinPoint.getSignature();

        Class<?> cls = codeSignature.getDeclaringType();
        String methodName = codeSignature.getName();
        String[] parameterNames = codeSignature.getParameterNames();
        Object[] parameterValues = joinPoint.getArgs();

        StringBuilder builder = new StringBuilder("\u21E2 ");
        builder.append(asTag(cls));
        builder.append("::");
        builder.append(methodName).append('(');
        for(int i = 0; i < parameterValues.length; i++)
        {
            if(i > 0)
            {
                builder.append(", ");
            }
            builder.append(parameterNames[i]).append('=');
            builder.append(Strings.toString(parameterValues[i]));
        }
        builder.append(')');

        /*
        if(Looper.myLooper() != Looper.getMainLooper())
        {
            builder.append(" [Thread:\"").append(Thread.currentThread().getName()).append("\"]");
        }*/

        Log.v(null, builder.toString());
    }

    private static void exitMethod(JoinPoint joinPoint, Object result, long lengthMillis)
    {
        Signature signature = joinPoint.getSignature();

        Class<?> cls = signature.getDeclaringType();
        String methodName = signature.getName();
        boolean hasReturnType = signature instanceof MethodSignature
                                        && ((MethodSignature) signature).getReturnType() != void.class;

        StringBuilder builder = new StringBuilder("\u21E0 ")
                                        .append(asTag(cls))
                                        .append("::")
                                        .append(methodName)
                                        .append(" [")
                                        .append(lengthMillis)
                                        .append("ms]");

        if(hasReturnType)
        {
            builder.append(" = ");
            builder.append(Strings.toString(result));
        }

        Log.v(null, builder.toString());
    }

    private static String asTag(Class<?> cls)
    {
        if(cls.isAnonymousClass())
        {
            return asTag(cls.getEnclosingClass());
        }
        return cls.getSimpleName();
    }
}
