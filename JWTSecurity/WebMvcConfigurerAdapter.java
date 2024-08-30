
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfigurerAdapter implements WebMvcConfigurer {

	private static final Logger log = LoggerFactory.getLogger(WebMvcConfigurerAdapter.class);
	
	@Override
	public void addCorsMappings(CorsRegistry registry) {
		log.info("Cors Mapping Enabled");
//		registry.addMapping("/**").allowCredentials(true).allowedMethods("GET", "POST", "PUT", "DELETE","OPTIONS")
//				.allowedOrigins("*")
//				.allowedHeaders("*");
		registry.addMapping("/**");
	}
	
}
