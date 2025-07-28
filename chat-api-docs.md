# VIMS 채팅 시스템 API 명세서

## 📡 WebSocket 연결
- **Endpoint**: `/ws`
- **Protocol**: SockJS + STOMP
- **연결 방법**: SockJS → STOMP over WebSocket

## 🏠 방 관리 (Room Management)

### REST API

#### 1. 방 생성
- **URL**: `POST /api/rooms/create`
- **설명**: 새로운 방(강의실)을 생성합니다
- **Request Body**:
```json
{
  "title": "자바 프로그래밍 수업",
  "description": "초급자를 위한 자바 기초 강의",
  "hostUserId": 123,
  "maxParticipants": 10,
  "password": "optional_password",
  "isRecordingEnabled": false,
  "scheduledStartTime": "2024-01-01T10:00:00",
  "scheduledEndTime": "2024-01-01T12:00:00",
  "isOpenToEveryone": false
}
```
- **Response**:
```json
{
  "roomId": 1,
  "roomCode": "ABC123",
  "title": "자바 프로그래밍 수업",
  "description": "초급자를 위한 자바 기초 강의",
  "hostUserId": 123,
  "maxParticipants": 10,
  "isRecordingEnabled": false,
  "createdAt": "2024-01-01T10:00:00",
  "autoJoin": true
}
```

#### 2. 방 정보 조회
- **URL**: `GET /api/rooms/{roomCode}`
- **설명**: 방 코드로 방 정보를 조회합니다
- **Parameters**:
  - `roomCode` (path): 방 코드
- **Response**:
```json
{
  "id": 1,
  "roomCode": "ABC123",
  "title": "자바 프로그래밍 수업",
  "description": "초급자를 위한 자바 기초 강의",
  "hostUserId": 123,
  "maxParticipants": 10,
  "currentParticipants": 5,
  "isRecordingEnabled": false,
  "createdAt": "2024-01-01T10:00:00",
  "scheduledStartTime": "2024-01-01T10:00:00",
  "scheduledEndTime": "2024-01-01T12:00:00",
  "status": "ACTIVE"
}
```

#### 3. 공개 방 목록 조회
- **URL**: `GET /api/rooms/open`
- **설명**: 공개된 모든 방 목록을 조회합니다
- **Response**:
```json
[
  {
    "id": 1,
    "roomCode": "ABC123",
    "title": "자바 프로그래밍 수업",
    "description": "초급자를 위한 자바 기초 강의",
    "hostUserId": 123,
    "maxParticipants": 10,
    "currentParticipants": 5,
    "isRecordingEnabled": false,
    "createdAt": "2024-01-01T10:00:00"
  }
]
```

#### 4. 사용자 방 목록 조회
- **URL**: `GET /api/rooms/user/{hostUserId}`
- **설명**: 특정 사용자가 생성한 방 목록을 조회합니다
- **Parameters**:
  - `hostUserId` (path): 호스트 사용자 ID
- **Response**:
```json
[
  {
    "id": 1,
    "roomCode": "ABC123",
    "title": "자바 프로그래밍 수업",
    "hostUserId": 123,
    "maxParticipants": 10,
    "createdAt": "2024-01-01T10:00:00"
  }
]
```

#### 5. 방 검색
- **URL**: `GET /api/rooms/search`
- **설명**: 키워드로 방을 검색합니다
- **Parameters**:
  - `keyword` (query): 검색 키워드
- **Response**:
```json
[
  {
    "id": 1,
    "roomCode": "ABC123",
    "title": "자바 프로그래밍 수업",
    "description": "초급자를 위한 자바 기초 강의",
    "hostUserId": 123,
    "maxParticipants": 10,
    "createdAt": "2024-01-01T10:00:00"
  }
]
```

## 💬 방 채팅 (Room Chat)

### WebSocket 메시지

#### 1. 방 입장
- **Destination**: `/app/room.join`
- **Method**: `SEND`
- **Payload**:
```json
{
  "roomCode": "ABC123",
  "userId": 123, 
  "userName": "김철수",
  "userRole": "INSTRUCTOR|STUDENT|TA"
}
```
- **필수 구독**: 
  - `/room/{roomCode}` - 방 전체 메시지 수신 (히스토리 + 실시간)
- **자동 처리**: 입장과 동시에 최근 50개 메시지 히스토리 자동 전송 (통합 메시지 형태)

#### 2. 방 퇴장  
- **Destination**: `/app/room.leave`
- **Method**: `SEND`
- **Payload**:
```json
{
  "roomCode": "ABC123",
  "userId": 123,
  "userName": "김철수"
}
```

#### 3. 방 메시지 전송
- **Destination**: `/app/room.send`
- **Method**: `SEND`
- **Payload**:
```json
{
  "roomCode": "ABC123",
  "senderId": 123,
  "senderName": "김철수", 
  "content": "안녕하세요!",
  "type": "CHAT|JOIN|LEAVE",
  "timestamp": "2024-01-01T10:00:00"
}
```
- **Message Types**:
  - `CHAT`: 일반 채팅 메시지
  - `JOIN`: 입장 메시지 (자동 생성)
  - `LEAVE`: 퇴장 메시지 (자동 생성)

#### 4. 방 메시지 수신
- **구독**: `/room/{roomCode}`
- **통합 메시지 형태** (히스토리와 실시간 모두 포함):
```json
{
  "type": "HISTORY_SYNC|REALTIME_MESSAGE|USER_JOIN|USER_LEAVE",
  "data": [메시지 배열 또는 단일 메시지],
  "timestamp": "2024-01-01T10:00:00"
}
```

#### 5. 메시지 형태 상세
- **히스토리 메시지**:
```json
{
  "type": "HISTORY_SYNC",
  "data": [
    {
      "id": 1,
      "roomId": 49,
      "senderId": 123,
      "messageType": "CHAT",
      "content": "메시지 내용",
      "createdAt": "2024-01-01T10:00:00"
    }
  ],
  "timestamp": "2024-01-01T10:00:00"
}
```

- **실시간 메시지**:
```json
{
  "type": "REALTIME_MESSAGE",
  "data": {
    "roomCode": "ABC123",
    "senderId": 123,
    "senderName": "김철수",
    "content": "메시지 내용",
    "type": "CHAT",
    "timestamp": "2024-01-01T10:00:00"
  }
}
```

### REST API

#### 1. 방 채팅 히스토리 조회 (페이징)
- **URL**: `GET /api/chat/room/history`
- **설명**: 추가 페이징이 필요한 경우 사용 (기본 히스토리는 WebSocket으로 자동 전송)
- **Parameters**:
  - `roomCode` (required): 방 코드
  - `page` (optional, default: 0): 페이지 번호  
  - `size` (optional, default: 50): 페이지 크기
- **Response**:
```json
[
  {
    "id": 1,
    "roomId": 49,
    "senderId": 123,
    "messageType": "CHAT", 
    "content": "메시지 내용",
    "createdAt": "2024-01-01T10:00:00"
  }
]
```

#### 2. 방 메시지 개수 조회
- **URL**: `GET /api/chat/room/message-count`
- **Parameters**:
  - `roomCode` (required): 방 코드
- **Response**: 
```json
25
```

#### 3. 방 참여자 목록 조회
- **URL**: `GET /api/chat/room/participants`
- **Parameters**:
  - `roomCode` (required): 방 코드
- **Response**:
```json
{
  "123": "김철수",
  "456": "이영희",
  "789": "박민수"
}
```

## 💬 다이렉트 메시지 (Direct Message)

### WebSocket 메시지

#### 1. DM 전송
- **Destination**: `/app/dm.send`
- **Method**: `SEND`
- **Payload**:
```json
{
  "senderId": "123",
  "senderName": "김철수",
  "receiverId": "456",
  "receiverName": "이영희",
  "content": "안녕하세요!"
}
```

#### 2. DM 방 입장 (히스토리 로드)
- **Destination**: `/app/dm.join`
- **Method**: `SEND`
- **Payload**:
```json
{
  "userId1": "123",
  "userId2": "456",
  "requesterId": "123"
}
```
- **필수 구독**: 
  - `/topic/dm.{roomId}` - DM 통합 메시지 수신 (히스토리 + 실시간)
  - `/user/queue/dm-notification` - DM 알림 수신
- **자동 처리**: 입장과 동시에 최근 50개 DM 히스토리 자동 전송 (통합 메시지 형태)

#### 3. DM 메시지 수신
- **구독**: `/topic/dm.{roomId}` (roomId = `{smallerId}_{largerId}`)
- **통합 메시지 형태** (히스토리와 실시간 모두 포함):
```json
{
  "category": "ROOM",
  "type": "DM_HISTORY_SYNC|DM_REALTIME",
  "payload": [메시지 배열 또는 단일 메시지],
  "timestamp": "2024-01-01T10:00:00"
}
```

#### 4. DM 메시지 형태 상세
- **DM 히스토리**:
```json
{
  "category": "ROOM",
  "type": "DM_HISTORY_SYNC",
  "payload": [
    {
      "id": 1,
      "senderId": 123,
      "receiverId": 456,
      "messageType": "DM",
      "content": "메시지 내용",
      "createdAt": "2024-01-01T10:00:00"
    }
  ],
  "timestamp": "2024-01-01T10:00:00"
}
```

- **DM 실시간 메시지**:
```json
{
  "category": "ROOM",
  "type": "DM_REALTIME",
  "payload": {
    "senderId": 123,
    "senderName": "김철수",
    "receiverId": 456,
    "receiverName": "이영희",
    "content": "메시지 내용",
    "type": "TEXT",
    "timestamp": "2024-01-01T10:00:00"
  },
  "timestamp": "2024-01-01T10:00:00"
}
```

### REST API

#### 1. DM 히스토리 조회
- **URL**: `GET /api/chat/dm/history`
- **Parameters**:
  - `userId1` (required): 사용자1 ID
  - `userId2` (required): 사용자2 ID
  - `page` (optional, default: 0): 페이지 번호
  - `size` (optional, default: 50): 페이지 크기
- **Response**:
```json
[
  {
    "id": 1,
    "senderId": 123,
    "receiverId": 456,
    "messageType": "DM",
    "content": "메시지 내용",
    "createdAt": "2024-01-01T10:00:00"
  }
]
```

#### 2. 최근 DM 상대방 목록 조회
- **URL**: `GET /api/chat/dm/recent-partners`
- **Parameters**:
  - `userId` (required): 사용자 ID
- **Response**:
```json
[
  [456, "2024-01-01T10:00:00"],
  [789, "2024-01-01T09:30:00"]
]
```

## 📋 메시지 타입

### RoomChatMessage.MessageType
- `CHAT`: 일반 채팅
- `JOIN`: 입장 메시지 (자동 생성)
- `LEAVE`: 퇴장 메시지 (자동 생성)

### DirectMessage.MessageType
- `TEXT`: 텍스트 메시지
- `FILE`: 파일 메시지
- `SYSTEM`: 시스템 메시지

### Message.MessageType (DB)
- `CHAT`: 일반 채팅
- `SYSTEM`: 시스템 메시지
- `DM`: 다이렉트 메시지

## 🔗 WebSocket 구독 패턴

### 🏆 권장 구조 (Clean & Simple)
```javascript
// 기본 구독 (앱 시작시)
1. `/user/queue/dm-notification`   // DM 알림

// 방 입장시 구독
2. `/room/{roomCode}`              // 방 통합 메시지 (히스토리+실시간)  

// DM 창 열 때 동적 구독
3. `/topic/dm.{roomId}`           // DM 통합 메시지 (히스토리+실시간)
```

### 📱 메시지 타입으로 구분
```javascript
// 방 메시지 타입들 (통합 메시지)
- HISTORY_SYNC     // 방 히스토리 동기화
- REALTIME_MESSAGE // 방 실시간 메시지  
- USER_JOIN        // 입장 알림
- USER_LEAVE       // 퇴장 알림

// DM 메시지 타입들 (통합 메시지)
- DM_HISTORY_SYNC  // DM 히스토리 동기화
- DM_REALTIME      // DM 실시간 메시지

// 알림 타입들
- DM_RECEIVED      // DM 수신 알림
```

### 📋 현재 구독 구조
1. **방 통합**: `/room/{roomCode}` - 방 히스토리와 실시간 통합
2. **DM 통합**: `/topic/dm.{roomId}` - DM 히스토리와 실시간 통합 (동적 구독)
3. **DM 알림**: `/user/queue/dm-notification` - DM 수신 알림

## 🛠️ 완전한 구현 예시

### JavaScript 클라이언트 예시

#### 1. WebSocket 연결 및 구독 설정
```javascript
const socket = new SockJS('/ws');
const stompClient = Stomp.over(socket);

// 메시지 순서 보장을 위한 큐잉 시스템
let messageQueue = [];
let historyLoading = false;

stompClient.connect({}, function(frame) {
    console.log('Connected: ' + frame);
});

function joinRoom() {
    const roomCode = 'ABC123';
    const userId = 123; 
    const userName = '김철수';
    const userRole = 'STUDENT';
    
    // 필수 구독 2개 설정
    // 1. 방 통합 메시지 (히스토리 + 실시간)
    stompClient.subscribe(`/room/${roomCode}`, function(message) {
        const unifiedMessage = JSON.parse(message.body);
        handleUnifiedMessage(unifiedMessage);
    });
    
    // 2. DM 알림
    stompClient.subscribe('/user/queue/dm-notification', function(notification) {
        const data = JSON.parse(notification.body);
        showDMNotification(data);
    });
    
    // 방 입장 메시지 전송
    stompClient.send('/app/room.join', {}, JSON.stringify({
        roomCode: roomCode,
        userId: userId,
        userName: userName, 
        userRole: userRole
    }));
}

// 통합 메시지 처리
function handleUnifiedMessage(unifiedMessage) {
    switch(unifiedMessage.type) {
        case 'HISTORY_SYNC':
            loadRoomHistory(unifiedMessage.data);
            break;
        case 'REALTIME_MESSAGE':
            displayRealtimeMessage(unifiedMessage.data);
            break;
        case 'USER_JOIN':
            displayJoinMessage(unifiedMessage.data);
            break;
        case 'USER_LEAVE':
            displayLeaveMessage(unifiedMessage.data);
            break;
    }
}
```

#### 2. 메시지 처리 시스템
```javascript
// 히스토리 로드
function loadRoomHistory(history) {
    // 기존 메시지 클리어
    clearMessages();
    
    // 히스토리를 시간순으로 정렬 후 표시
    const sortedHistory = history.sort((a, b) => 
        new Date(a.createdAt) - new Date(b.createdAt)
    );
    
    sortedHistory.forEach(msg => {
        displayHistoryMessage(msg);
    });
}

// 실시간 메시지 표시
function displayRealtimeMessage(message) {
    displayMessageInternal(message);
}

// 입장/퇴장 메시지 처리
function displayJoinMessage(message) {
    displaySystemMessage(`${message.senderName}님이 입장했습니다.`);
}

function displayLeaveMessage(message) {
    displaySystemMessage(`${message.senderName}님이 퇴장했습니다.`);
}
```

#### 3. 메시지 전송
```javascript
// 일반 채팅 메시지
function sendMessage(content) {
    stompClient.send('/app/room.send', {}, JSON.stringify({
        roomCode: currentRoomCode,
        senderId: currentUserId,
        senderName: currentUserName,
        content: content,
        type: 'CHAT',
        timestamp: new Date().toISOString()
    }));
}
```

#### 4. DM 기능 (통합 메시지 방식)
```javascript
// DM 창 열기 (통합 구독)
function openDM(targetUserId, targetUserName) {
    const roomId = generateDMRoomId(currentUserId, targetUserId);
    
    // DM 통합 구독 (히스토리 + 실시간)
    stompClient.subscribe(`/topic/dm.${roomId}`, function(message) {
        const unifiedMessage = JSON.parse(message.body);
        handleDMUnifiedMessage(unifiedMessage);
    });
    
    // DM 히스토리 요청
    stompClient.send('/app/dm.join', {}, JSON.stringify({
        userId1: currentUserId.toString(),
        userId2: targetUserId.toString(),
        requesterId: currentUserId.toString()
    }));
}

// DM 통합 메시지 처리
function handleDMUnifiedMessage(unifiedMessage) {
    switch(unifiedMessage.type) {
        case 'DM_HISTORY_SYNC':
            loadDMHistory(unifiedMessage.payload);
            break;
        case 'DM_REALTIME':
            displayDMRealtime(unifiedMessage.payload);
            break;
    }
}

// DM 전송
function sendDM(content, receiverId, receiverName) {
    stompClient.send('/app/dm.send', {}, JSON.stringify({
        senderId: currentUserId,
        senderName: currentUserName,
        receiverId: receiverId,
        receiverName: receiverName,
        content: content
    }));
}
```

#### 5. 추가 페이징 (필요시)
```javascript
// 더 많은 히스토리 로드 (REST API 사용)
async function loadMoreHistory(page = 1) {
    try {
        const response = await fetch(
            `/api/chat/room/history?roomCode=${currentRoomCode}&page=${page}&size=50`
        );
        const history = await response.json();
        prependHistoryMessages(history); // 위쪽에 추가
    } catch (error) {
        console.error('Failed to load more history:', error);
    }
}
```

## 📝 핵심 특징 및 주의사항

### ✨ 핵심 특징
1. **통합 메시지 시스템**: 히스토리와 실시간 메시지가 단일 채널로 통합
2. **WebSocket 기반 히스토리**: 방 입장과 동시에 최근 50개 메시지 자동 전송 
3. **동적 구독**: DM 방은 필요할 때만 구독하여 효율성 확보
4. **실시간 알림**: DM 수신 시 즉시 알림 (창이 닫혀있어도)
5. **간소화된 구독**: 필수 구독 2개로 간소화

### ⚠️ 주의사항
1. **구독 순서**: 방 입장 전에 반드시 필수 구독을 먼저 설정
2. **히스토리 메커니즘**: 
   - 기본 히스토리: WebSocket으로 자동 전송 (50개, 통합 메시지)
   - 추가 히스토리: REST API로 페이징 조회 
3. **메시지 타입 처리**: 통합 메시지의 타입에 따른 적절한 처리 필요
4. **DM Room ID**: 사용자 ID를 정렬하여 생성 (`dm_{smaller}_{larger}`)
5. **사용자 인증**: 현재는 단순 ID 기반 (실운영시 JWT 등 인증 구현 필요)
6. **세션 관리**: WebSocket 연결 상태 관리 필수

### 🔄 실행 순서 (중요!)
```
1. WebSocket 연결
2. 필수 구독 설정 (방 통합 채널 + DM 알림)
3. 방 입장 메시지 전송  
4. 백엔드에서 히스토리 자동 전송 (통합 메시지)
5. 실시간 메시지 처리 시작
```

## 🔧 기술 스택

- **Backend**: Spring Boot 3.x, Spring WebSocket, STOMP Protocol
- **Database**: MySQL 8.0 (메시지 저장)
- **Cache**: Redis (세션 관리, 참여자 관리)
- **Frontend**: SockJS, STOMP.js
- **Message Broker**: Spring Simple Broker
- **API**: WebSocket (주) + REST API (보조)