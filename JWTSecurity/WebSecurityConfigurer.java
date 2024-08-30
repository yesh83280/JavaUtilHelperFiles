
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;


//Spring Security Configuration class
@Configuration
@EnableWebSecurity
//@EnableWebMvc
@EnableMethodSecurity(
		  prePostEnabled = true, 
		  securedEnabled = true)
public class WebSecurityConfigurer {
	
	@Autowired
	private JWTAuthorizationFilter jwtAuthorizationFilter;

	private final Logger log = LoggerFactory.getLogger(WebSecurityConfigurer.class);

	@Bean
	protected SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http.csrf(csrf -> csrf.disable())
		.addFilterAfter(jwtAuthorizationFilter, UsernamePasswordAuthenticationFilter.class)
		.authorizeHttpRequests(auth -> auth.requestMatchers(HttpMethod.POST, "/Login").permitAll()
				.requestMatchers(HttpMethod.GET, "/health/ping").permitAll()
				.requestMatchers(HttpMethod.GET, "/getSSOEnable/flagStatus").permitAll()
				.requestMatchers(HttpMethod.GET, "/getCurrentDate").permitAll()
				.requestMatchers(HttpMethod.GET, "/proxy").permitAll()
				.requestMatchers("/error").permitAll()
				.anyRequest().authenticated());
		
		http.cors(Customizer.withDefaults());
		
		log.info("Configured WebSecurityConfig");

		return http.build();

	}


}
