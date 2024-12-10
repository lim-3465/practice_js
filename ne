React 클라이언트가 서버에서 Access Token을 handle_redirect URL로 POST 요청을 통해 받았을 때, 이를 처리하고 적절히 저장하려면 다음 단계를 따릅니다.

1. Access Token 처리 흐름
	1.	Authorization Server가 handle_redirect URL로 POST 요청을 전송하며 Authorization Code 또는 Access Token을 포함.
	2.	React 클라이언트는 handle_redirect 페이지를 렌더링하여 Access Token을 추출.
	3.	Access Token을 안전한 저장소(Local Storage, Session Storage, HttpOnly Cookie 등)에 저장.
	4.	이후 API 요청에서 Access Token을 인증 헤더에 포함.

2. Access Token 처리 코드

2.1 React의 handle_redirect 컴포넌트

React 컴포넌트에서 URL에 포함된 Access Token 또는 Authorization Code를 추출하고 저장합니다.

import React, { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';

export const HandleRedirect = () => {
  const navigate = useNavigate();

  useEffect(() => {
    const handleRedirect = async () => {
      // Authorization Code 또는 Access Token 추출
      const params = new URLSearchParams(window.location.search);
      const code = params.get('code'); // Authorization Code
      const accessToken = params.get('access_token'); // Access Token (if directly provided)

      if (accessToken) {
        // Access Token 저장 (간단한 경우)
        localStorage.setItem('accessToken', accessToken);
        navigate('/dashboard'); // 대시보드로 이동
      } else if (code) {
        try {
          // Authorization Code를 서버로 전달하여 Access Token 요청
          const response = await axios.post('/handle_redirect', { code });
          const { accessToken } = response.data;

          // Access Token 저장
          localStorage.setItem('accessToken', accessToken);
          navigate('/dashboard'); // 대시보드로 이동
        } catch (error) {
          console.error('Error exchanging code for access token:', error);
          navigate('/login'); // 로그인 페이지로 이동
        }
      } else {
        console.error('Authorization code or access token not found');
        navigate('/login');
      }
    };

    handleRedirect();
  }, [navigate]);

  return <div>Processing authentication...</div>;
};

2.2 Access Token 저장 위치

React에서는 Access Token을 다음 위치 중 하나에 저장합니다:
	1.	Local Storage:
	•	구현이 간단하지만 XSS 공격에 취약.

localStorage.setItem('accessToken', accessToken);


	2.	Session Storage:
	•	브라우저 세션이 끝날 때 토큰 삭제.

sessionStorage.setItem('accessToken', accessToken);


	3.	HttpOnly 쿠키:
	•	보안성이 높고 XSS 공격에 안전.
	•	서버에서 Set-Cookie 헤더를 사용하여 설정해야 함.

// Spring Boot 예제
private void setAccessTokenCookie(String accessToken, HttpServletResponse response) {
    Cookie cookie = new Cookie("accessToken", accessToken);
    cookie.setHttpOnly(true);
    cookie.setSecure(true);
    cookie.setPath("/");
    cookie.setMaxAge(3600); // 1시간
    response.addCookie(cookie);
}

3. 백엔드: Authorization Code 처리

handle_redirect에서 Authorization Code를 Access Token으로 교환하는 엔드포인트를 구현합니다.

Spring Boot 컨트롤러

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Map;

@RestController
public class OAuthController {

    @Value("${thirdparty.client-id}")
    private String clientId;

    @Value("${thirdparty.client-secret}")
    private String clientSecret;

    @Value("${thirdparty.redirect-uri}")
    private String redirectUri;

    @Value("${thirdparty.token-uri}")
    private String tokenUri;

    private final RestTemplate restTemplate;

    public OAuthController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @PostMapping("/handle_redirect")
    public ResponseEntity<Map<String, Object>> handleRedirect(@RequestParam("code") String code, HttpServletResponse response) {
        try {
            // Authorization Code를 Access Token으로 교환
            String accessToken = requestAccessToken(code);

            // Access Token을 HttpOnly 쿠키에 저장 (선택 사항)
            setAccessTokenCookie(accessToken, response);

            return ResponseEntity.ok(Map.of("accessToken", accessToken));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
        }
    }

    private String requestAccessToken(String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        String body = String.format(
            "grant_type=authorization_code&code=%s&redirect_uri=%s&client_id=%s&client_secret=%s",
            code, redirectUri, clientId, clientSecret
        );

        HttpEntity<String> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                tokenUri,
                HttpMethod.POST,
                request,
                Map.class
        );

        if (response.getStatusCode() == HttpStatus.OK) {
            return (String) response.getBody().get("access_token");
        } else {
            throw new RuntimeException("Failed to retrieve access token");
        }
    }

    private void setAccessTokenCookie(String accessToken, HttpServletResponse response) {
        Cookie cookie = new Cookie("accessToken", accessToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(true); // HTTPS만 허용
        cookie.setPath("/");
        cookie.setMaxAge(3600); // 1시간
        response.addCookie(cookie);
    }
}

4. 최종 결과
	1.	클라이언트에서 처리:
	•	React 클라이언트는 Access Token을 저장(Local Storage, Session Storage 또는 HttpOnly Cookie)하고 사용자 인증 상태를 관리.
	2.	백엔드에서 처리:
	•	Authorization Code를 Access Token으로 교환.
	•	필요하면 HttpOnly 쿠키로 Access Token을 클라이언트에 전달.

5. 권장 사항
	1.	보안 강화를 위해 HttpOnly 쿠키 사용:
	•	Access Token을 HttpOnly 쿠키에 저장하면 XSS 공격을 방지할 수 있습니다.
	2.	Refresh Token 사용:
	•	Access Token이 만료되면 Refresh Token으로 새 Access Token을 발급받는 기능을 추가하세요.
	3.	로그인 상태 확인:
	•	React에서 사용자 상태를 전역적으로 관리(Context API, Redux 등)하여 로그인 상태를 확인하세요.















아래는 엔드포인트에서 /oauth2/handle_redirect를 단순히 /handle_redirect로 변경한 코드입니다. 관련된 모든 경로 및 호출 부분을 수정했습니다.

Spring Boot 컨트롤러

1. Authorization Code 처리

엔드포인트를 /handle_redirect로 수정한 코드:

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@RestController
public class OAuthController {

    @Value("${thirdparty.client-id}")
    private String clientId;

    @Value("${thirdparty.client-secret}")
    private String clientSecret;

    @Value("${thirdparty.redirect-uri}")
    private String redirectUri;

    @Value("${thirdparty.token-uri}")
    private String tokenUri;

    private final RestTemplate restTemplate;

    public OAuthController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @GetMapping("/handle_redirect")
    public ResponseEntity<Map<String, Object>> handleRedirect(@RequestParam("code") String code) {
        // Authorization Code를 Access Token으로 교환
        String accessToken = requestAccessToken(code);

        return ResponseEntity.ok(Map.of("accessToken", accessToken));
    }

    private String requestAccessToken(String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        String body = String.format(
            "grant_type=authorization_code&code=%s&redirect_uri=%s&client_id=%s&client_secret=%s",
            code, redirectUri, clientId, clientSecret
        );

        HttpEntity<String> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                tokenUri,
                HttpMethod.POST,
                request,
                Map.class
        );

        if (response.getStatusCode() == HttpStatus.OK) {
            return (String) response.getBody().get("access_token");
        } else {
            throw new RuntimeException("Failed to retrieve access token");
        }
    }
}

React 클라이언트

2. React의 Callback URL 수정

React에서 인증 서버로부터 리다이렉트된 Authorization Code를 처리하기 위해 /handle_redirect를 호출하도록 수정합니다.

import React, { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';

export const OAuthCallback = () => {
  const navigate = useNavigate();

  useEffect(() => {
    const handleOAuthCallback = async () => {
      const params = new URLSearchParams(window.location.search);
      const code = params.get('code');

      if (code) {
        try {
          const response = await axios.get(`/handle_redirect?code=${code}`);
          const { accessToken } = response.data;

          // Access Token 저장
          localStorage.setItem('accessToken', accessToken);

          // 대시보드로 이동
          navigate('/dashboard');
        } catch (error) {
          console.error('Error during OAuth callback:', error);
          navigate('/login');
        }
      } else {
        console.error('Authorization code not found');
        navigate('/login');
      }
    };

    handleOAuthCallback();
  }, [navigate]);

  return <div>Processing OAuth callback...</div>;
};

3. Redirect URI 수정

React 클라이언트에서 인증 요청 시 redirect_uri를 /handle_redirect로 설정합니다.

const clientId = 'your-client-id';
const redirectUri = encodeURIComponent('http://localhost:8080/handle_redirect');
const authorizationUrl = `https://third-party-auth.com/oauth/authorize?response_type=code&client_id=${clientId}&redirect_uri=${redirectUri}&scope=openid profile email`;

window.location.href = authorizationUrl; // OAuth 인증 서버로 리다이렉트

최종 흐름 요약
	1.	React 클라이언트:
	•	로그인 버튼 클릭 시, OAuth 인증 서버로 리다이렉트.
	•	redirect_uri를 /handle_redirect로 설정.
	2.	서드파티 인증 서버:
	•	인증 후, Authorization Code와 함께 클라이언트를 /handle_redirect로 리다이렉트.
	3.	Spring Boot 백엔드:
	•	/handle_redirect 엔드포인트에서 Authorization Code를 처리.
	•	Access Token을 발급받


자동 로그인을 구현하려면, 사용자가 애플리케이션을 방문했을 때 이미 Access Token이나 Refresh Token이 저장되어 있는지 확인하고, 저장된 토큰을 사용해 사용자를 자동으로 인증 처리합니다. OAuth2 인증 흐름을 유지하면서 React와 Spring Boot를 사용해 자동 로그인을 구현하는 방법을 설명하겠습니다.

1. React: Access Token 확인 및 자동 로그인

1.1 자동 로그인 흐름
	1.	사용자가 애플리케이션에 방문했을 때:
	•	Local Storage(또는 HttpOnly 쿠키)에 저장된 Access Token 확인.
	•	Access Token이 유효하면 사용자 정보를 가져와 자동 로그인.
	•	Access Token이 없거나 만료되었으면 OAuth 인증 흐름을 시작.

1.2 React 코드

import React, { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';

export const App = () => {
  const navigate = useNavigate();

  useEffect(() => {
    const checkLogin = async () => {
      const accessToken = localStorage.getItem('accessToken');

      if (accessToken) {
        try {
          // Access Token으로 사용자 정보 확인
          const response = await axios.get('/api/userinfo', {
            headers: {
              Authorization: `Bearer ${accessToken}`,
            },
          });

          if (response.status === 200) {
            // 사용자 정보가 유효하면 대시보드로 이동
            navigate('/dashboard');
          }
        } catch (error) {
          console.error('Access token expired or invalid:', error);

          // Access Token이 만료되었으면 로그인 흐름 시작
          startLoginFlow();
        }
      } else {
        // Access Token이 없으면 로그인 흐름 시작
        startLoginFlow();
      }
    };

    const startLoginFlow = () => {
      const clientId = 'your-client-id';
      const redirectUri = encodeURIComponent('http://localhost:8080/oauth2/callback');
      const authorizationUrl = `https://third-party-auth.com/oauth/authorize?response_type=code&client_id=${clientId}&redirect_uri=${redirectUri}&scope=openid profile email`;

      window.location.href = authorizationUrl; // OAuth 인증 서버로 리다이렉트
    };

    checkLogin();
  }, [navigate]);

  return <div>Loading...</div>;
};

1.3 Callback 처리

OAuth 인증 후 리다이렉트된 redirect_uri에서 Access Token을 저장하고 대시보드로 이동합니다.

import React, { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';

export const OAuthCallback = () => {
  const navigate = useNavigate();

  useEffect(() => {
    const handleOAuthCallback = async () => {
      const params = new URLSearchParams(window.location.search);
      const code = params.get('code');

      if (code) {
        try {
          const response = await axios.get(`/oauth2/callback?code=${code}`);
          const { accessToken } = response.data;

          // Access Token 저장
          localStorage.setItem('accessToken', accessToken);

          // 대시보드로 이동
          navigate('/dashboard');
        } catch (error) {
          console.error('Error during OAuth callback:', error);
          navigate('/login');
        }
      } else {
        console.error('Authorization code not found');
        navigate('/login');
      }
    };

    handleOAuthCallback();
  }, [navigate]);

  return <div>Processing OAuth callback...</div>;
};

2. Spring Boot: 사용자 정보 및 Access Token 검증

Spring Boot에서 Access Token을 검증하고 사용자 정보를 반환하는 API를 구현합니다.

2.1 Access Token 검증

/api/userinfo 엔드포인트에서 Access Token을 확인하고, 유효하면 사용자 정보를 반환합니다.

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserInfoController {

    @GetMapping("/api/userinfo")
    public ResponseEntity<?> getUserInfo(@RequestHeader("Authorization") String authorizationHeader) {
        // Access Token 검증
        String token = authorizationHeader.replace("Bearer ", "");

        try {
            // Access Token 검증 로직 (JWT 파싱 또는 서드파티 인증 서버 호출)
            String username = validateAccessToken(token);

            // 사용자 정보 반환
            return ResponseEntity.ok(Map.of(
                "username", username,
                "roles", List.of("ROLE_USER")
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired token");
        }
    }

    private String validateAccessToken(String token) {
        // 예: JWT 파싱 및 유효성 검증 로직
        if (token.isEmpty() || !token.startsWith("valid")) {
            throw new RuntimeException("Invalid token");
        }
        return "user@example.com"; // 디코딩된 사용자 이름
    }
}

2.2 Authorization Code 처리

Authorization Code를 Access Token으로 교환하여 클라이언트에 반환합니다.

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@RestController
@RequestMapping("/oauth2")
public class OAuth2Controller {

    @Value("${thirdparty.client-id}")
    private String clientId;

    @Value("${thirdparty.client-secret}")
    private String clientSecret;

    @Value("${thirdparty.redirect-uri}")
    private String redirectUri;

    @Value("${thirdparty.token-uri}")
    private String tokenUri;

    private final RestTemplate restTemplate;

    public OAuth2Controller(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @GetMapping("/callback")
    public ResponseEntity<Map<String, Object>> handleOAuthCallback(@RequestParam("code") String code) {
        // Authorization Code를 Access Token으로 교환
        String accessToken = requestAccessToken(code);

        return ResponseEntity.ok(Map.of("accessToken", accessToken));
    }

    private String requestAccessToken(String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        String body = String.format(
            "grant_type=authorization_code&code=%s&redirect_uri=%s&client_id=%s&client_secret=%s",
            code, redirectUri, clientId, clientSecret
        );

        HttpEntity<String> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                tokenUri,
                HttpMethod.POST,
                request,
                Map.class
        );

        if (response.getStatusCode() == HttpStatus.OK) {
            return (String) response.getBody().get("access_token");
        } else {
            throw new RuntimeException("Failed to retrieve access token");
        }
    }
}

3. 자동 로그인 흐름 요약
	1.	React 초기 로드:
	•	Local Storage에서 Access Token 확인.
	•	유효한 Access Token이 있다면 /api/userinfo로 사용자 정보를 요청.
	•	Access Token이 없거나 만료되었다면 OAuth 인증 흐름 시작.
	2.	Spring Boot:
	•	/api/userinfo: Access Token을 검증하고 사용자 정보 반환.
	•	/oauth2/callback: Authorization Code를 Access Token으로 교환.
	3.	React의 상태 업데이트:
	•	사용자 정보 로드 후 대시보드로 이동.
	•	Access Token이 유효하지 않을 경우 로그인 페이지로 리다이렉트.

추가 요청 가능

a. Refresh Token을 사용한 자동 Access Token 갱신 기능 추가 요청.
b. 사용자 역할(Role) 기반의 화면 접근 제어 추가 요청.



A. 인증 실패 시 상세 오류 메시지 반환 및 UI 처리

OAuth 인증 프로세스에서 인증 실패 시 사용자에게 적절한 오류 메시지를 반환하고 표시하는 방법을 구현합니다. 여기에서는 Spring Boot와 React를 기반으로 한 해결 방법을 다룹니다.

1. Spring Boot: 인증 실패 처리

Spring Boot 백엔드에서 OAuth 인증 실패 시 상세한 오류 메시지를 반환합니다.

1.1 컨트롤러 코드 수정

Spring Boot 컨트롤러에서 requestAccessToken 메서드에 예외 처리를 추가하여 인증 실패 시 적절한 메시지를 반환합니다.

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@RestController
public class OAuthController {

    @Value("${thirdparty.client-id}")
    private String clientId;

    @Value("${thirdparty.client-secret}")
    private String clientSecret;

    @Value("${thirdparty.redirect-uri}")
    private String redirectUri;

    @Value("${thirdparty.token-uri}")
    private String tokenUri;

    private final RestTemplate restTemplate;

    public OAuthController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @GetMapping("/handle_redirect")
    public ResponseEntity<Map<String, Object>> handleRedirect(@RequestParam("code") String code) {
        try {
            // Authorization Code를 Access Token으로 교환
            String accessToken = requestAccessToken(code);
            return ResponseEntity.ok(Map.of("accessToken", accessToken));
        } catch (Exception e) {
            // 인증 실패 시 상세한 오류 메시지 반환
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
        }
    }

    private String requestAccessToken(String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        String body = String.format(
            "grant_type=authorization_code&code=%s&redirect_uri=%s&client_id=%s&client_secret=%s",
            code, redirectUri, clientId, clientSecret
        );

        HttpEntity<String> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                tokenUri,
                HttpMethod.POST,
                request,
                Map.class
        );

        if (response.getStatusCode() == HttpStatus.OK) {
            return (String) response.getBody().get("access_token");
        } else {
            throw new RuntimeException("Failed to retrieve access token: " + response.getStatusCode());
        }
    }
}

2. React: 인증 실패 메시지 표시

React 클라이언트에서 인증 실패 시 Spring Boot에서 반환된 오류 메시지를 화면에 표시합니다.

2.1 OAuthCallback 컴포넌트 수정

OAuthCallback 컴포넌트에서 인증 실패 메시지를 표시하도록 수정합니다.

import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';

export const OAuthCallback = () => {
  const navigate = useNavigate();
  const [errorMessage, setErrorMessage] = useState(null);

  useEffect(() => {
    const handleOAuthCallback = async () => {
      const params = new URLSearchParams(window.location.search);
      const code = params.get('code');

      if (code) {
        try {
          const response = await axios.get(`/handle_redirect?code=${code}`);
          const { accessToken } = response.data;

          // Access Token 저장
          localStorage.setItem('accessToken', accessToken);

          // 대시보드로 이동
          navigate('/dashboard');
        } catch (error) {
          console.error('Error during OAuth callback:', error);
          // 오류 메시지를 상태에 저장
          setErrorMessage(error.response?.data?.error || 'An unexpected error occurred.');
        }
      } else {
        setErrorMessage('Authorization code not found. Please try logging in again.');
      }
    };

    handleOAuthCallback();
  }, [navigate]);

  return (
    <div>
      {errorMessage ? (
        <div>
          <h1>Authentication Failed</h1>
          <p>{errorMessage}</p>
          <button onClick={() => navigate('/login')}>Back to Login</button>
        </div>
      ) : (
        <div>Processing OAuth callback...</div>
      )}
    </div>
  );
};

3. React의 로그인 컴포넌트

사용자가 오류 메시지를 본 후 다시 로그인할 수 있도록 로그인 페이지로 이동하는 버튼을 제공합니다.

import React from 'react';

export const Login = () => {
  const handleLogin = () => {
    const clientId = 'your-client-id';
    const redirectUri = encodeURIComponent('http://localhost:8080/handle_redirect');
    const authorizationUrl = `https://third-party-auth.com/oauth/authorize?response_type=code&client_id=${clientId}&redirect_uri=${redirectUri}&scope=openid profile email`;

    window.location.href = authorizationUrl; // 인증 서버로 리다이렉트
  };

  return (
    <div>
      <h1>Login</h1>
      <button onClick={handleLogin}>Login with OAuth</button>
    </div>
  );
};

4. 흐름 요약
	1.	React의 /handle_redirect 요청:
	•	Authorization Code를 전달하여 Access Token 요청.
	•	Spring Boot가 Access Token 요청 실패 시, 오류 메시지를 클라이언트로 반환.
	2.	React의 오류 메시지 표시:
	•	Spring Boot에서 반환된 오류 메시지를 읽어 사용자에게 표시.
	•	“Back to Login” 버튼을 통해 로그인 페이지로 돌아갈 수 있도록 구현.

테스트
	1.	정상 인증:
	•	React가 Authorization Code를 /handle_redirect로 전달하여 Access Token을 성공적으로 획득.
	•	사용자 대시보드로 리디렉트.
	2.	인증 실패:
	•	잘못된 Authorization Code를 전달하거나 Access Token 요청이 실패하면 React에서 오류 메시지를 표시.

추가 작업 제안

a. 인증 실패 시 오류 유형별 메시지(예: 네트워크 문제, 만료된 코드 등)를 구분하여 사용자에게 알림.
b. 백엔드와 프론트엔드에서 인증 로그(Audit Trail)를 저장하도록 구현.


Spring Boot와 React를 사용하여 사용자 역할(Role)별로 API와 화면 접근을 제한하는 방법을 구현해보겠습니다.

1. Spring Boot: API 역할(Role) 기반 접근 제한

Spring Security를 사용하여 API 접근을 특정 역할로 제한합니다.

1.1 Spring Security 설정

SecurityConfig.java

Spring Security에서 역할(Role)별로 API를 보호합니다.

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/public/**").permitAll() // 공개 API
                .requestMatchers("/admin/**").hasRole("ADMIN") // ADMIN 역할만 접근 가능
                .requestMatchers("/user/**").hasRole("USER") // USER 역할만 접근 가능
                .anyRequest().authenticated() // 나머지 요청은 인증 필요
            );

        return http.build();
    }
}

1.2 JWT에 역할 포함

JWT를 생성할 때 사용자의 역할(Role)을 포함합니다.

JwtTokenProvider.java

import io.jsonwebtoken.*;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

@Component
public class JwtTokenProvider {

    private final String SECRET_KEY = "your-secret-key";
    private final long EXPIRATION_TIME = 3600000; // 1시간

    public String generateToken(String username, List<String> roles) {
        return Jwts.builder()
                .setSubject(username)
                .claim("roles", roles) // 역할(Role) 추가
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(SignatureAlgorithm.HS256, SECRET_KEY)
                .compact();
    }

    public List<String> getRolesFromToken(String token) {
        return Jwts.parser()
                .setSigningKey(SECRET_KEY)
                .parseClaimsJws(token)
                .getBody()
                .get("roles", List.class);
    }
}

1.3 JWT 필터에서 역할 인증

JWT에서 역할(Role)을 추출하고 Spring Security의 SecurityContext에 설정합니다.

JwtAuthenticationFilter.java

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.stream.Collectors;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String token = authorizationHeader.substring(7);

            try {
                if (jwtTokenProvider.validateToken(token)) {
                    String username = jwtTokenProvider.getUsernameFromToken(token);
                    var roles = jwtTokenProvider.getRolesFromToken(token);

                    var authorities = roles.stream()
                            .map(SimpleGrantedAuthority::new)
                            .collect(Collectors.toList());

                    var authentication = new UsernamePasswordAuthenticationToken(username, null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (JwtException e) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
        }

        chain.doFilter(request, response);
    }
}

1.4 API 컨트롤러

API 컨트롤러에서 Spring Security의 역할 기반 보호를 확인합니다.

AdminController.java

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
public class AdminController {

    @GetMapping("/dashboard")
    public String adminDashboard() {
        return "Welcome to Admin Dashboard!";
    }
}

UserController.java

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
public class UserController {

    @GetMapping("/dashboard")
    public String userDashboard() {
        return "Welcome to User Dashboard!";
    }
}

2. React: 역할(Role)별 화면 접근 제한

React에서 사용자의 역할(Role)에 따라 특정 화면에 접근을 제한합니다.

2.1 사용자 정보 가져오기

React에서 사용자의 역할(Role)을 가져와 상태로 관리합니다.

useUserInfo 커스텀 훅

import { useEffect, useState } from 'react';
import axios from 'axios';

export const useUserInfo = () => {
  const [userInfo, setUserInfo] = useState(null);

  useEffect(() => {
    const fetchUserInfo = async () => {
      const token = localStorage.getItem('accessToken');

      if (token) {
        try {
          const response = await axios.get('/api/userinfo', {
            headers: {
              Authorization: `Bearer ${token}`,
            },
          });

          setUserInfo(response.data);
        } catch (error) {
          console.error('Error fetching user info:', error);
          setUserInfo(null);
        }
      }
    };

    fetchUserInfo();
  }, []);

  return userInfo;
};

2.2 역할(Role)별 화면 렌더링

React 라우팅 설정

사용자의 역할(Role)에 따라 접근 권한이 제한된 컴포넌트를 렌더링합니다.

import React from 'react';
import { BrowserRouter as Router, Route, Routes, Navigate } from 'react-router-dom';
import { useUserInfo } from './useUserInfo';
import { AdminDashboard } from './AdminDashboard';
import { UserDashboard } from './UserDashboard';
import { Login } from './Login';

export const App = () => {
  const userInfo = useUserInfo();

  if (!userInfo) {
    return <Login />;
  }

  return (
    <Router>
      <Routes>
        {/* Admin Role */}
        {userInfo.roles.includes('ROLE_ADMIN') && (
          <Route path="/admin" element={<AdminDashboard />} />
        )}

        {/* User Role */}
        {userInfo.roles.includes('ROLE_USER') && (
          <Route path="/user" element={<UserDashboard />} />
        )}

        {/* Default Redirect */}
        <Route path="*" element={<Navigate to={userInfo.roles.includes('ROLE_ADMIN') ? "/admin" : "/user"} />} />
      </Routes>
    </Router>
  );
};

2.3 예제 대시보드 컴포넌트

Admin Dashboard

import React from 'react';

export const AdminDashboard = () => {
  return (
    <div>
      <h1>Admin Dashboard</h1>
      <p>Welcome, Admin!</p>
    </div>
  );
};

User Dashboard

import React from 'react';

export const UserDashboard = () => {
  return (
    <div>
      <h1>User Dashboard</h1>
      <p>Welcome, User!</p>
    </div>
  );
};

3. 결과
	1.	API 역할 제한:
	•	/admin/**: ROLE_ADMIN만 접근 가능.
	•	/user/**: ROLE_USER만 접근 가능.
	2.	React 화면 역할 제한:
	•	ROLE_ADMIN: Admin Dashboard 접근 가능.
	•	ROLE_USER: User Dashboard 접근 가능.

추가 요청 가능

a. 로그인한 사용자의 세션 만료 처리 및 로그아웃 기능 추가.
b. Role 관리 API 구현(추가, 삭제, 업데이트).

