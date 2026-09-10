package com.example1.getyourride.service.impl;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example1.getyourride.dto.request.StudentLoginRequest;
import com.example1.getyourride.dto.request.StudentRegisterRequest;
import com.example1.getyourride.dto.response.AuthResponse;
import com.example1.getyourride.entity.Driver;
import com.example1.getyourride.entity.Student;
import com.example1.getyourride.repository.DriverRepository;
import com.example1.getyourride.repository.StudentRepository;
import com.example1.getyourride.security.JwtUtil;

class StudentAuthServiceImplTest {

    @Mock
    private StudentRepository studentRepo;
    @Mock
    private DriverRepository driverRepo;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private StudentAuthServiceImpl studentAuthService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void register_Success() {
        StudentRegisterRequest request = new StudentRegisterRequest();
        request.setStudentNumber("S12345678");
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setEmail("john.doe@mandela.ac.za");
        request.setPhone("0821234567");
        request.setPassword("password");
        request.setIsFunded(true);

        when(studentRepo.findByEmail(anyString())).thenReturn(Optional.empty());
        when(driverRepo.findByEmail(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        
        Student savedStudent = new Student();
        savedStudent.setStudentId(1L);
        savedStudent.setStudentNumber(request.getStudentNumber());
        savedStudent.setEmail(request.getEmail());
        savedStudent.setFirstName(request.getFirstName());
        savedStudent.setLastName(request.getLastName());
        savedStudent.setPhone(request.getPhone());
        savedStudent.setIsFunded(true);
        
        when(studentRepo.save(any(Student.class))).thenReturn(savedStudent);
        when(jwtUtil.generateToken(anyLong(), anyString(), anyString(), anyMap())).thenReturn("token");

        AuthResponse response = studentAuthService.register(request);

        assertNotNull(response);
        assertEquals("token", response.getToken());
        assertEquals("STUDENT", response.getType());
        assertEquals(1L, response.getId());
        assertEquals("john.doe@mandela.ac.za", response.getEmail());
        assertEquals("S12345678", response.getStudentNumber());
        assertEquals(request.getPhone(), response.getPhone());
        assertTrue(response.getIsFunded());

        verify(studentRepo, times(1)).save(any(Student.class));
    }

    @Test
    void register_EmailAlreadyExists() {
        StudentRegisterRequest request = new StudentRegisterRequest();
        request.setEmail("john.doe@mandela.ac.za");

        when(studentRepo.findByEmail(request.getEmail())).thenReturn(Optional.of(new Student()));

        assertThrows(IllegalStateException.class, () -> studentAuthService.register(request));
    }

    @Test
    void register_StudentNumberAlreadyExists() {
        StudentRegisterRequest request = new StudentRegisterRequest();
        request.setStudentNumber("S12345678");
        request.setEmail("john.doe@mandela.ac.za");

        when(studentRepo.findByEmail(anyString())).thenReturn(Optional.empty());
        when(driverRepo.findByEmail(anyString())).thenReturn(Optional.empty());
        when(studentRepo.existsByStudentNumber("S12345678")).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> studentAuthService.register(request));
    }

    @Test
    void login_Success() {
        StudentLoginRequest request = new StudentLoginRequest();
        request.setEmail("john.doe@mandela.ac.za");
        request.setPassword("password");

        Student student = new Student();
        student.setStudentId(1L);
        student.setEmail("john.doe@mandela.ac.za");
        student.setPassword("encodedPassword");
        student.setFirstName("John");
        student.setLastName("Doe");
        student.setIsFunded(true);

        when(studentRepo.findByEmail(request.getEmail())).thenReturn(Optional.of(student));
        when(passwordEncoder.matches(request.getPassword(), student.getPassword())).thenReturn(true);
        when(jwtUtil.generateToken(anyLong(), anyString(), anyString(), anyMap())).thenReturn("token");
        
        student.setStudentNumber("S12345678");
        student.setPhone("0821234567");

        AuthResponse response = studentAuthService.login(request);

        assertNotNull(response);
        assertEquals("token", response.getToken());
        assertEquals(1L, response.getId());
        assertEquals("S12345678", response.getStudentNumber());
        assertEquals("0821234567", response.getPhone());
        verify(studentRepo, times(1)).findByEmail(request.getEmail());
    }

    @Test
    void login_InvalidPassword() {
        StudentLoginRequest request = new StudentLoginRequest();
        request.setEmail("john.doe@mandela.ac.za");
        request.setPassword("wrongpassword");

        Student student = new Student();
        student.setPassword("encodedPassword");

        when(studentRepo.findByEmail(request.getEmail())).thenReturn(Optional.of(student));
        when(passwordEncoder.matches(request.getPassword(), student.getPassword())).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> studentAuthService.login(request));
    }

    @Test
    void login_PlainTextPasswordSuccess() {
        StudentLoginRequest request = new StudentLoginRequest();
        request.setEmail("kelvin@mandela.ac.za");
        request.setPassword("test123");

        Student student = new Student();
        student.setStudentId(1L);
        student.setEmail("kelvin@mandela.ac.za");
        student.setPassword("test123"); // Plain text in DB
        student.setFirstName("Kelvin");
        student.setLastName("Mudzingwa");

        when(studentRepo.findByEmail(request.getEmail())).thenReturn(Optional.of(student));
        // BCrypt fails, but plain text matches
        when(passwordEncoder.matches(request.getPassword(), student.getPassword())).thenReturn(false);
        when(jwtUtil.generateToken(anyLong(), anyString(), anyString(), anyMap())).thenReturn("token");

        AuthResponse response = studentAuthService.login(request);

        assertNotNull(response);
        assertEquals("token", response.getToken());
    }
    
    @Test
    void login_ShuttleDriver_ReturnsShuttleDriverTypeForBoardingRoute() {
        StudentLoginRequest request = new StudentLoginRequest();
        request.setEmail("shuttle.driver@mandela.ac.za");
        request.setPassword("password");

        Driver shuttleDriver = new Driver();
        shuttleDriver.setDriverId(4L);
        shuttleDriver.setEmail(request.getEmail());
        shuttleDriver.setPassword("encodedPassword");
        shuttleDriver.setFirstName("Shuttle");
        shuttleDriver.setLastName("Driver");
        shuttleDriver.setRole("SHUTTLE_DRIVER");
        shuttleDriver.setIsVerified(true);

        when(studentRepo.findByEmail(request.getEmail())).thenReturn(Optional.empty());
        when(driverRepo.findByEmail(request.getEmail())).thenReturn(Optional.of(shuttleDriver));
        when(passwordEncoder.matches(request.getPassword(), shuttleDriver.getPassword())).thenReturn(true);
        // Raw value, not anyMap(): Mockito rejects mixing matchers with raw arguments in one call,
        // and the other three arguments here are raw. Using the same map the verify below expects
        // also makes the stub assert the claims, which anyMap() did not.
        when(jwtUtil.generateToken(4L, request.getEmail(), "SHUTTLE_DRIVER", Map.of("role", "SHUTTLE_DRIVER")))
                .thenReturn("token");

        AuthResponse response = studentAuthService.login(request);

        assertEquals("SHUTTLE_DRIVER", response.getType());
        assertEquals("SHUTTLE_DRIVER", response.getRole());
        verify(jwtUtil).generateToken(4L, request.getEmail(), "SHUTTLE_DRIVER", Map.of("role", "SHUTTLE_DRIVER"));
    }

    @Test
    void login_UserNotFound() {
        StudentLoginRequest request = new StudentLoginRequest();
        request.setEmail("notfound@mandela.ac.za");
        request.setPassword("password");

        when(studentRepo.findByEmail(request.getEmail())).thenReturn(Optional.empty());
        when(driverRepo.findByEmail(request.getEmail())).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> studentAuthService.login(request));
    }
}
