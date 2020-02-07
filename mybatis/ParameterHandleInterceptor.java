package com.ens.core.mybatis.interceptor;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.PreparedStatement;
import java.util.Properties;

import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.scripting.defaults.DefaultParameterHandler;
import org.apache.ibatis.type.JdbcType;
import org.springframework.stereotype.Component;

/**
 * Discarded.
 * This interceptor's intend was to force null value of VARCHAR types to change to empty string value.
 * Class worked as intended, but typehandler with includeNullJdbcType option was way more standard and 
 * simple. So It has deprecated and kept in case of needs.
 * @author emuggie
 *
 */
@Intercepts(value = { @Signature(type = ParameterHandler.class, method = "setParameters", args = { PreparedStatement.class} ) })
@Component
public class ParameterHandleInterceptor implements Interceptor{

	@Override
	public Object intercept(Invocation invocation) throws Throwable {
		if(!(invocation.getTarget() instanceof DefaultParameterHandler)){
			return invocation.proceed();
		}
		DefaultParameterHandler target =(DefaultParameterHandler)invocation.getTarget();
		PreparedStatement ps = (PreparedStatement)invocation.getArgs()[0];
		PreparedStatement psProxy = (PreparedStatement) Proxy.newProxyInstance(
			ps.getClass().getClassLoader()
			, ps.getClass().getInterfaces()
			, new PsProxy(ps));
		target.setParameters(psProxy);
		return null;
	}

	@Override
	public Object plugin(Object target) {
		return Plugin.wrap(target, this);
	}

	@Override
	public void setProperties(Properties properties) {}
	
	private class PsProxy implements InvocationHandler {
		private Object target;
		private Method setString;
		public PsProxy(Object target){
			this.target = target;
			try {
				this.setString = this.target.getClass().getMethod("setString", int.class, String.class);
			} catch (NoSuchMethodException | SecurityException e) {
				this.setString = null;
				e.printStackTrace();
			}
		}
		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			if("setString".equals(method.getName())){
				if(args[1] == null){
					System.out.println("Replace Null to empty from setString");
					return method.invoke(this.target, args[0], args[1]);
				}
			}else if("setNull".equals(method.getName())){
				try{
					if(this.setString != null && JdbcType.VARCHAR.TYPE_CODE == (int)args[1]){
						System.out.println("Replace Null to empty from setNull");
						return this.setString.invoke(this.target, args[0], "");
					}
				}catch(Exception err){
					return method.invoke(this.target, args);
				}
			}
			return method.invoke(this.target, args);
		}
	}
}