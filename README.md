**MediConnect**
# Video Call Module

## 📖 Overview
The **Video Call module** enables real-time virtual consultations between patients and doctors.  
It integrates with the **Appointment API** to display upcoming virtual appointments and uses **WebRTC** on the client side to support peer-to-peer video communication.

## 🛠 Tech Stack
- **React v18**
- **Tailwind CSS**
- **WebRTC** (Client-side video communication)
- **Spring Boot Backend** (Appointment API)
- **REST API Integration**

## 🎯 Core Responsibilities
- Display upcoming virtual appointments for patients and doctors  
- Enable peer-to-peer video calls using WebRTC  
- Provide role-specific views for patients and doctors  
- Allow doctors to complete consultations  
- Keep patient and doctor views synchronized using backend state  

## 📡 Signaling in Backend
While WebRTC handles the actual peer-to-peer media exchange, it requires a **signaling mechanism** to coordinate the connection setup between patient and doctor.  
In this module, signaling is managed by the **Spring Boot backend**:

- **Session Initialization**: When a virtual appointment is fetched, the backend provides a unique session identifier for the call.
- **Offer/Answer Exchange**: The patient’s browser sends an SDP (Session Description Protocol) offer to the backend, which relays it to the doctor’s client. The doctor responds with an SDP answer, also routed through the backend.
- **ICE Candidate Handling**: Both clients generate ICE candidates (network connection details). These are sent to the backend via REST/WebSocket endpoints and forwarded to the peer client.
- **Synchronization with Appointment API**: The backend ensures that signaling events are tied to the correct appointment record, keeping patient and doctor views consistent.
- **Completion Flow**: Once the doctor ends the call, the backend updates the appointment status (`COMPLETED`) and clears signaling state.

This design ensures:
- Reliable message delivery even if one client temporarily disconnects.  
- Tight integration with the **Appointment API** for state management.  
- A foundation for future enhancements like authentication, reconnection handling, and analytics.

## 🔄 Module Flow

### 1. Appointment Creation
Virtual appointments are created using the Appointment API with consultation mode set to `VIRTUAL` and status set to `UPCOMING`.

### 2. Fetching Virtual Appointments
Both patient and doctor video call pages fetch upcoming virtual appointments using the backend API:
```
GET /api/appointments/virtual
```

### 3. WebRTC Video Call
The React application includes WebRTC logic to establish a peer-to-peer connection between patient and doctor.  
This includes:
- Media stream access  
- Peer connection setup  
- Signaling handled within the frontend code  

### 4. Completing the Call
After the consultation, the doctor can mark the appointment as completed.  
This updates the appointment status in the backend and removes it from the upcoming video call list:
```
PUT /api/appointments/{id}/complete
```

## 🔗 API Dependencies
- `GET /api/appointments/virtual`  
- `PUT /api/appointments/{id}/complete`  

## 📝 Design Notes
- Relies on **WebRTC** for real-time communication.  
- Synchronization is managed via backend appointment status.  
- No authentication implemented; data exposure is controlled through scoped APIs.  

## 🚀 Future Enhancements
- Authentication and role-based access  
- Call duration tracking  
- Automatic appointment completion  
- Recording and call analytics  
- Improved signaling and reconnection handling  

## ✅ Status
Implemented with WebRTC integration in the React application.  
Ready for further enhancements.

## ▶️ Commands to Run the Code

### Backend (Spring Boot)
```bash
# Navigate to backend folder
cd backend

# Build and run with Maven
mvn clean install
mvn spring-boot:run