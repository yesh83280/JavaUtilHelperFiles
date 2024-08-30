
import java.security.Key;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import javax.crypto.spec.SecretKeySpec;


// JWT Utility Class - to validate, generate, get details of/from the JWT Tokens
@Component
public class JWTUtils{

	private final String PREFIX = "Bearer ";

	//	It is very important to have a strong SECRET and change once in a while (not too often)
	private final String SECRET;

	private final long TOKEN_EXPIRY;

	private final Key signingKey;

	JWTUtils(@Value("${app.jwt.secret}") String secret, @Value("${app.jwt.expiry}") long expiry){
		SECRET = secret;
		TOKEN_EXPIRY = expiry;
		signingKey = new SecretKeySpec(SECRET.getBytes(), SignatureAlgorithm.HS512.getJcaName());
	}

	//	To Generate JWT Tokens
	public String getJWTToken(LoginUserDTO userState) {
		List<String> authorities = generateAuthorities(userState.getUserNodeList());

		String token = Jwts
				.builder()
//				.setId("JWTToken")
				.setSubject(userState.getEmpintra())
//				Sets authority/roles that a User can do
				.claim("authorities", authorities)
//				Sets LoginUser details to the token
				.claim("userState", userState)
				.setIssuedAt(new Date(System.currentTimeMillis()))
//				Sets Expiry to the Token
				.setExpiration(new Date(System.currentTimeMillis() + TOKEN_EXPIRY))
//				Signed with a secret key - for authentication
				.signWith(signingKey).compact();

		return PREFIX + token;
	}

	//	To check whether we have a token in the header
	public boolean checkJWTToken(String token) {
		return token != null && token.startsWith(PREFIX);
	}

	//	To validate the authenticity and expiration of token
	public Claims validateToken(String token) {
		String jwtToken = token.replace(PREFIX, "");
		return Jwts.parserBuilder().setSigningKey(signingKey).build().parseClaimsJws(jwtToken).getBody();
	}

	//	Prefixes ROLE_ to all userNodeList Strings.
	public List<String> generateAuthorities(List<String> authList) {
		return authList.stream().map(auth -> "ROLE_" + auth).collect(Collectors.toList());
	}

}