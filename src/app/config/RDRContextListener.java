package app.config;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.javalite.instrumentation.Instrumentation;

public class RDRContextListener implements ServletContextListener {
	private ServletContext context = null;

	public void contextDestroyed(ServletContextEvent event) {
		// Output a simple message to the server'sconsole
		System.out.println("The Simple Web App. Has BeenRemoved");
		this.context = null;
	}

	// 这个方法在Web应用服务做好接受请求的时候被调用。
	public void contextInitialized(ServletContextEvent event) {
		this.context = event.getServletContext();

		Instrumentation instrumentation = new Instrumentation();
		String path = this.context.getRealPath("");
		System.out.println(path+"/WEB-INF/classes");
		instrumentation.setOutputDirectory(path+"/WEB-INF/classes");
		instrumentation.instrument();

		System.out.println("The Simple Web App. IsReady");
	}

}
