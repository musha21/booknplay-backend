package lk.booknplay;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;
import lk.booknplay.security.jwt.JwtProperties;

@SpringBootApplication
@EnableAsync
@EnableConfigurationProperties(JwtProperties.class)
public class BooknplayApplication {

	public static void main(String[] args) {
		// #region agent log
		try {
			boolean fqcnPresent = false;
			String fqcnError = null;
			try {
				Class.forName("org.springframework.mail.javamail.JavaMailSender");
				fqcnPresent = true;
			} catch (Throwable t) {
				fqcnError = t.getClass().getSimpleName() + ":" + t.getMessage();
			}
			String ctorGeneric = null;
			String genericParam0 = null;
			String ctorInspectError = null;
			try {
				java.lang.reflect.Constructor<?> ctor = Class.forName("lk.booknplay.service.impl.NotificationServiceImpl").getDeclaredConstructors()[0];
				ctorGeneric = ctor.toGenericString();
				java.lang.reflect.Type[] types = ctor.getGenericParameterTypes();
				genericParam0 = types.length > 0 ? types[0].getTypeName() : "none";
			} catch (Throwable t) {
				ctorInspectError = t.getClass().getName() + ":" + t.getMessage();
				if (t.getCause() != null) {
					ctorInspectError += "|" + t.getCause().getClass().getSimpleName() + ":" + t.getCause().getMessage();
				}
			}
			String mailJar = "absent";
			try {
				java.net.URL url = Thread.currentThread().getContextClassLoader().getResource("org/springframework/mail/javamail/JavaMailSender.class");
				mailJar = url == null ? "absent" : url.toString();
			} catch (Throwable ignored) {
			}
			String payload = "{\"sessionId\":\"5b0412\",\"runId\":\"post-fix\",\"hypothesisId\":\"A\",\"location\":\"BooknplayApplication.java:main\",\"message\":\"mail classpath and NotificationServiceImpl ctor\",\"data\":{\"fqcnPresent\":" + fqcnPresent + ",\"fqcnError\":\"" + String.valueOf(fqcnError) + "\",\"ctorGeneric\":\"" + String.valueOf(ctorGeneric).replace("\"", "'") + "\",\"genericParam0\":\"" + String.valueOf(genericParam0).replace("\"", "'") + "\",\"ctorInspectError\":\"" + String.valueOf(ctorInspectError).replace("\"", "'") + "\",\"mailJar\":\"" + mailJar.replace("\\", "/").replace("\"", "'") + "\"},\"timestamp\":" + System.currentTimeMillis() + "}\n";
			java.nio.file.Files.writeString(java.nio.file.Path.of("c:/Users/Musharaf/Downloads/booknplay/booknplay/debug-5b0412.log"), payload, java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
		} catch (Exception ignored) {
		}
		// #endregion
		SpringApplication.run(BooknplayApplication.class, args);
	}

}

