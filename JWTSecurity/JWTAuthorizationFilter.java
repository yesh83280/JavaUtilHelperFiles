
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;

//Executes everytime a REST Request comes
@Component
public class JWTAuthorizationFilter extends OncePerRequestFilter {


	@Autowired
	private JWTUtils jwtUtils;

	@Autowired
	private ObjectMapper mapper;

	private final String HEADER = "Authorization";
	
	private static final Logger log = LoggerFactory.getLogger(JWTAuthorizationFilter.class);
	
	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws IOException {
		log.info(request.getRequestURI());

		try {
			String token = request.getHeader(HEADER);
			log.info("Token availability: " + jwtUtils.checkJWTToken(token));

//			Check for valid JWT Token - authentication
			if (jwtUtils.checkJWTToken(token)) {
				log.info("Token validity: " + (jwtUtils.validateToken(token) != null ));
				Claims claims = jwtUtils.validateToken(token);
				if (claims.get("authorities") != null) {
					setUpSpringAuthentication(claims);
				} else {
					SecurityContextHolder.clearContext();
				}
			} else {
				SecurityContextHolder.clearContext();
			}
			filterChain.doFilter(request, response);
		} catch (ExpiredJwtException | UnsupportedJwtException | MalformedJwtException e) {
			log.error("Token validity: false \nToken validity failure Message: " + e.getMessage());
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
		} catch(Exception e){
			log.error("Internal Server Error. Message: " + e.getMessage());
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}

	//	Sets up Spring Security Context - to be used in Controller to know who performs certain tasks
	private void setUpSpringAuthentication(Claims claims) {
		String[] authorities = mapper.convertValue(claims.get("authorities"), String[].class);
		List<GrantedAuthority> grantAuthorities = Arrays.stream(authorities)
				.map(SimpleGrantedAuthority::new)
				.collect(Collectors.toList());

		LoginUserDTO userState = mapper.convertValue(claims.get("userState"), LoginUserDTO.class);

		UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
				claims.getSubject(),
				null, grantAuthorities);

		auth.setDetails(userState);
		SecurityContextHolder.getContext().setAuthentication(auth);
	}


}
