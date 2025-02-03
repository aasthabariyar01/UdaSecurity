package com.udacity.catpoint.SecurityService;

import com.udacity.catpoint.SecurityService.service.SecurityService;
import com.udacity.catpoint.ImageService.ImageService;
import com.udacity.catpoint.SecurityService.data.*;
import com.udacity.catpoint.SecurityService.application.StatusListener;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;

import java.awt.image.BufferedImage;
import java.util.Set;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
public class SecurityServiceTest {

    @Mock
    private Sensor sensor1;
    @Mock
    private Sensor sensor2;
    @Mock
    private SecurityRepository securityRepository;
    @Mock
    private ImageService imageService;
    @Mock
    private BufferedImage bufferedImage;
    @Mock
    private Set<Sensor> sensorSet;
    @Mock
    private StatusListener statusListener1;
    @Mock
    private StatusListener statusListener2;
    @InjectMocks
    private SecurityService securityService;

    @BeforeEach
    void initialize() {
        this.securityService = new SecurityService(securityRepository, imageService);
    }


    @Test
    public void shouldReturnTrue() {
        assertTrue(true);
    }

    @Test
    public void shouldSetAlarmStatusToPendingWhenSensorIsActivated() {
        ArmingStatus armingStatus = ArmingStatus.ARMED_HOME;
        AlarmStatus currentAlarmStatus = AlarmStatus.NO_ALARM;
        when(securityRepository.getArmingStatus()).thenReturn(armingStatus);
        when(securityRepository.getAlarmStatus()).thenReturn(currentAlarmStatus);
        securityService.changeSensorActivationStatus(sensor1, true);
        verify(securityRepository).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }

    @Test
    public void shouldSetAlarmStatusToAlarmWhenSensorActivatedInPendingState() {
        ArmingStatus armingStatus = ArmingStatus.ARMED_HOME;
        AlarmStatus currentAlarmStatus = AlarmStatus.PENDING_ALARM;
        when(securityRepository.getArmingStatus()).thenReturn(armingStatus);
        when(securityRepository.getAlarmStatus()).thenReturn(currentAlarmStatus);
        securityService.changeSensorActivationStatus(sensor1, true);
        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test
    public void shouldReturnNoAlarmStatusWhenAllSensorsAreInactive() {
        AlarmStatus initialStatus = AlarmStatus.PENDING_ALARM;
        when(securityRepository.getAlarmStatus()).thenReturn(initialStatus);
        when(sensor1.getActive()).thenReturn(true);
        securityService.changeSensorActivationStatus(sensor1, false);
        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @Test
    public void shouldNotChangeAlarmStatusIfAlarmIsActive() {
        lenient().when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        securityService.changeSensorActivationStatus(sensor1, false);
        verify(securityRepository, never()).setAlarmStatus(any());

    }
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"DISARMED","ARMED_HOME", "ARMED_AWAY"})
    public void shouldSetAlarmIfCatDetectedInArmedHomeState() {
        mockArmingStatus(ArmingStatus.ARMED_HOME);
        mockCatDetected(true);
        securityService.processImage(bufferedImage);
        verifyAlarmStatusSetTo(AlarmStatus.ALARM);
    }
    private void mockArmingStatus(ArmingStatus status) {
        when(securityRepository.getArmingStatus()).thenReturn(status);
    }
    private void mockCatDetected(boolean detected) {
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(detected);
    }
    private void verifyAlarmStatusSetTo(AlarmStatus expectedStatus) {
        verify(securityRepository).setAlarmStatus(expectedStatus);
    }

    @Test
    public void shouldSetNoAlarmIfSystemIsDisarmed() {
        securityService.setArmingStatus(ArmingStatus.DISARMED);
        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @Test
    public void shouldSetAllSensorsToInactiveWhenSystemIsArmed() {
        when(securityService.getSensors()).thenReturn(Set.of(sensor1, sensor2));
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);
        verify(sensor1).setActive(false);
        verify(sensor2).setActive(false);
    }

    @Test
    public void shouldNotTriggerAlarmIfCatIsDetectedButSystemIsNotArmedHome() {
        ArmingStatus currentArmingStatus = ArmingStatus.ARMED_AWAY;
        boolean catDetected = true;
        when(securityRepository.getArmingStatus()).thenReturn(currentArmingStatus);
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(catDetected);
        securityService.processImage(bufferedImage);
        verify(securityRepository, never()).setAlarmStatus(AlarmStatus.ALARM);
    }

    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"DISARMED","ARMED_HOME", "ARMED_AWAY"})
    public void shouldSetNoAlarmWhenNoCatDetectedAndAllSensorsAreInactive() {
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(false);
        when(securityRepository.getSensors()).thenReturn(Set.of(sensor1, sensor2));
        configureSensorInactive(sensor1);
        configureSensorInactive(sensor2);
        securityService.processImage(bufferedImage);
        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }
    private void configureSensorInactive(Sensor sensor) {
        when(sensor.getActive()).thenReturn(false);
    }

    @Test
    public void shouldNotifyStatusListenerOnSensorStatusChange() {
        securityService.addStatusListener(statusListener1);
        securityService.changeSensorActivationStatus(sensor1, true);
        verify(statusListener1).sensorStatusChanged();
    }
}
