# VIMS - Video Conference System

WebRTC 기반 화상회의 시스템입니다.

## 📁 프로젝트 구조

```
BE-VIMS/
├── docker-compose.yml          # 전체 서비스 orchestration
├── backend/                    # Spring Boot 애플리케이션
│   ├── src/
│   ├── build.gradle.kts
│   └── Dockerfile
├── coturn/                     # COTURN 설정
│   └── turnserver.conf
├── mysql/                      # MySQL 초기화 스크립트
│   └── init.sql
├── kurento/                    # Kurento 설정 (확장용)
└── README.md
```

## 🚀 실행 방법

### 전체 서비스 시작
```bash
docker-compose up -d
```

### 로그 확인
```bash
# 전체 로그
docker-compose logs -f

# 특정 서비스 로그
docker-compose logs -f vims-app
docker-compose logs -f kurento
docker-compose logs -f coturn
```

### 서비스 중지
```bash
docker-compose down
```

### 데이터 볼륨까지 삭제
```bash
docker-compose down -v
```

## 🌐 서비스 포트

| 서비스 | 포트 | 용도 |
|--------|------|------|
| Spring Boot | 8080 | 웹 애플리케이션 |
| MySQL | 3306 | 데이터베이스 |
| Redis | 6379 | 세션 관리 |
| Kurento | 8888 | 미디어 서버 |
| COTURN | 3478, 5349 | STUN/TURN 서버 |

## 🔧 개발 환경

### Spring Boot만 로컬에서 실행
```bash
# 외부 서비스들만 실행
docker-compose up -d mysql redis kurento coturn

# Spring Boot 로컬 실행
cd backend
./gradlew bootRun
```

### 특정 서비스 재시작
```bash
docker-compose restart vims-app
```

## 📋 기본 계정

**MySQL**
- Root: `root` / `rootpass`
- App User: `vims` / `vimspass`

**COTURN**
- Username: `turnuser`
- Password: `turnpass`

## 🌍 접속 URL

- **애플리케이션**: http://localhost:8080
- **채팅 테스트**: http://localhost:8080/chat-test.html
- **H2 Console** (개발시): http://localhost:8080/h2-console

## ⚠️ 주의사항

1. **포트 충돌**: 각 서비스의 포트가 이미 사용 중이지 않은지 확인
2. **방화벽**: COTURN의 UDP 포트 범위(49152-65535)가 방화벽에서 열려있는지 확인
3. **메모리**: Kurento Media Server는 상당한 메모리를 사용하므로 충분한 리소스 확보 필요

## 🛠️ 트러블슈팅

### 서비스 상태 확인
```bash
docker-compose ps
```

### 컨테이너 내부 접속
```bash
docker-compose exec vims-app bash
docker-compose exec mysql mysql -u vims -p
```

### 볼륨 초기화 (데이터 리셋)
```bash
docker-compose down -v
docker volume prune
docker-compose up -d
```
