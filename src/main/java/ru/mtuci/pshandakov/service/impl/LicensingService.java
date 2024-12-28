package ru.mtuci.pshandakov.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.mtuci.pshandakov.exception.LicenseException;
import ru.mtuci.pshandakov.exception.UserEmptyException;
import ru.mtuci.pshandakov.model.ActionAuthRegHistory;
import ru.mtuci.pshandakov.model.ActionAuthRegHistoryTypes;
import ru.mtuci.pshandakov.model.ApplicationUser;
import ru.mtuci.pshandakov.model.Device;
import ru.mtuci.pshandakov.model.DeviceLicense;
import ru.mtuci.pshandakov.model.License;
import ru.mtuci.pshandakov.model.LicenseActivationRequest;
import ru.mtuci.pshandakov.model.LicenseCheckRequest;
import ru.mtuci.pshandakov.model.LicenseCreateRequest;
import ru.mtuci.pshandakov.model.LicenseCreateResponse;
import ru.mtuci.pshandakov.model.LicenseResponse;
import ru.mtuci.pshandakov.model.LicenseType;
import ru.mtuci.pshandakov.model.LicenseUpdateRequest;
import ru.mtuci.pshandakov.model.Product;
import ru.mtuci.pshandakov.model.Ticket;
import ru.mtuci.pshandakov.repository.ActionAuthRegHistoryRepository;
import ru.mtuci.pshandakov.repository.DeviceLicenseRepository;
import ru.mtuci.pshandakov.utils.DateTimeUtil;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LicensingService {
    private final ApplicationUserService applicationUserService;
    private final ProductService productService;
    private final LicenseTypeService licenseTypeService;
    private final LicenseService licenseService;
    private final LicenseHistoryService licenseHistoryService;
    private final DeviceService deviceService;

    private final ActionAuthRegHistoryRepository actionAuthRegHistoryRepository;

    private final DeviceLicenseRepository deviceLicenseRepository;

    private void logLicensing(String email, String username, String description, String action) {
        ActionAuthRegHistory historyEntry = new ActionAuthRegHistory();
        historyEntry.setEmail(email);
        historyEntry.setUsername(username);
        historyEntry.setTimeAction(LocalDateTime.now());
        historyEntry.setDescription(description);
        historyEntry.setActionType(action);
        actionAuthRegHistoryRepository.save(historyEntry);
    }

    private void logLicensing(String email, String username, String description) {
        logLicensing(email, username, description, ActionAuthRegHistoryTypes.LICENSE.getType());
    }

    private Product productCheck(ApplicationUser user, Long productId, boolean createTicket) throws LicenseException {
        log.info("Проверка существования продукта с ID: {}", productId);
        var product = productService.getProductById(productId);

        if (product.isEmpty()) {
            throw new LicenseException("Продукт не найден", createTicket);
        }

        log.info("Продукт найден: {}", product.get().getName());

        if (product.get().isBlocked()) {
            log.warn("Продукт с ID: {} заблокирован", productId);
            String msg = "Ошибка создания лицензии: продукт лицензии заблокирован";
            logLicensing(user.getEmail(), user.getUsername(), msg);
            throw new LicenseException(msg, false);
        }

        return product.get();
    }

    private ApplicationUser ownerCheck(Long ownerId, boolean createTicket) throws LicenseException {
        log.info("Проверка существования пользователя с ID: {}", ownerId);
        var owner = applicationUserService.getUserById(ownerId);

        if (owner.isEmpty()) {
            throw new LicenseException("Пользователь не найден", createTicket);
        }

        log.info("Пользователь найден: {}", ownerId);
        return owner.get();
    }

    private LicenseType licenseTypeCheck(Long licenseTypeId, boolean createTicket) throws LicenseException {
        log.info("Проверка существования типа лицензии с ID: {}", licenseTypeId);
        var licenseType = licenseTypeService.getLicenseTypeById(licenseTypeId);

        if (licenseType.isEmpty()) {
            throw new LicenseException("Тип лицензии не найден", createTicket);
        }

        log.info("Тип лицензии найден: {}", licenseTypeId);
        return licenseType.get();
    }

    private License licenseCheck(String licenseCode, boolean createTicket) throws LicenseException {
        var license = licenseService.getLicenseByCode(licenseCode);

        if (license.isEmpty()) {
            throw new LicenseException("Недействительный ключ лицензии", true);
        }

        log.info("Лицензия с кодом {} найдена", licenseCode);
        return license.get();
    }

    private void ownerCompareCheck(ApplicationUser licenseOwner, ApplicationUser requestUser) throws LicenseException {
        if (!licenseOwner.getId().equals(requestUser.getId())) {
            logLicensing(requestUser.getEmail(), requestUser.getUsername(), "Ошибка обновления лицензии: попытка обновления лицензии будучи !owner");
            throw new LicenseException("Ошибка: пользователь не является владельцем лицензии", false);
        }
    }

    private String generateActivationCode() {
        final int codeLength = 32;
        final String allowedChars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

        SecureRandom random = new SecureRandom();
        return random.ints(allowedChars.length(), 0, allowedChars.length())
                .limit(codeLength)
                .mapToObj(i -> String.valueOf(allowedChars.charAt(i)))
                .collect(Collectors.joining());
    }

    public LicenseCreateResponse createLicense(ApplicationUser user, LicenseCreateRequest requestData) throws Exception {
        if (user == null) {
            throw new UserEmptyException();
        }

        try {
            var product = productCheck(user, requestData.getProductId(), false);
            var owner = ownerCheck(requestData.getOwnerId(), false);
            var licenseType = licenseTypeCheck(requestData.getLicenseTypeId(), false);

            log.info("Создание новой лицензии...");
            License newLicense = new License();
            newLicense.setCode(generateActivationCode());
            log.info("Активационный код сгенерирован: {}", newLicense.getCode());

            newLicense.setOwner(owner);
            newLicense.setProduct(product);
            newLicense.setLicenseType(licenseType);

            newLicense.setBlocked(false);

            newLicense.setDeviceCount(requestData.getDeviceCount());
            newLicense.setDescription(requestData.getDescription() != null ? requestData.getDescription() : "Лицензия успешно создана: " + user.getUsername());

            log.info("Сохранение лицензии в базе данных...");
            licenseService.createLicense(newLicense);
            log.info("Лицензия успешно сохранена в базе данных с ID: {}", newLicense.getId());

            String description = "Лицензия создана";
            Date changeDate = DateTimeUtil.convertLocalDateToDate(LocalDate.now());
            licenseHistoryService.recordLicenseChange(newLicense.getId(), owner.getId(), "Создана", changeDate, description);
            log.info("Запись изменений лицензии в историю завершена");
            logLicensing(user.getEmail(), user.getUsername(), "Лицензия была создана: " + user.getUsername());

            return LicenseCreateResponse.builder().message("Лицензия успешно создана").build();
        } catch (LicenseException e) {
            log.error("Ошибка при создании лицензии: {}", e.getMessage(), e);
            logLicensing(user.getEmail(), user.getUsername(), "Ошибка создания лицензии: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Произошла ошибка при создании лицензии: {}", e.getMessage(), e);
            logLicensing(user.getEmail(), user.getUsername(), "Ошибка создания лицензии: " + e.getMessage(), "Лицензия");
            throw e;
        }
    }

    public LicenseResponse updateLicense(ApplicationUser user, LicenseUpdateRequest requestData) throws Exception {
        if (user == null) {
            throw new UserEmptyException();
        }

        try {
            License license = licenseCheck(requestData.getCode(), true);

            if (license.getFirstActivationDate() == null) {
                logLicensing(user.getEmail(), user.getUsername(), "Ошибка обновления лицензии: Лицензия еще активирована");
                throw new LicenseException("Лицензия еще не активирована, продление невозможно.", false);
            }

            ownerCompareCheck(license.getOwner(), user);

            if (license.getBlocked()) {
                log.warn("Лицензия с кодом {} заблокирована", requestData.getCode());
                Ticket ticket = Ticket.createTicket(license.getOwner().getId(), true, null, "Лицензия заблокирована");
                log.info("Тикет: {}", ticket);
                logLicensing(user.getEmail(), user.getUsername(), "Ошибка обновления лицензии: попытка обновления заблокированной лицензии");
                return LicenseResponse.builder()
                        .ticket(ticket)
                        .build();
            }

            if (license.getEndingDate().before(new Date())) {
                log.warn("Лицензия с кодом {} уже истекла", requestData.getCode());
                Ticket ticket = Ticket.createTicket(license.getOwner().getId(), false, null, "Лицензия истекла, продление невозможно.");
                log.info("Тикет: {}", ticket);
                return LicenseResponse.builder()
                        .ticket(ticket)
                        .build();
            }

            Date newExpirationDate = DateTimeUtil.parseFromString("yyyy-MM-dd'T'HH:mm:ss", requestData.getNewExpirationDate());
            log.info("Новая дата окончания: {}", newExpirationDate);

            if (newExpirationDate.compareTo(license.getEndingDate()) <= 0) {
                log.warn("Новая дата окончания лицензии {} не может быть меньше или равна текущей дате окончания {}", requestData.getNewExpirationDate(), license.getEndingDate());
                Ticket ticket = Ticket.createTicket(license.getOwner().getId(), false, null, "Новая дата окончания лицензии не может быть меньше или равна текущей");
                log.info("Тикет: {}", ticket);
                return LicenseResponse.builder()
                        .ticket(ticket)
                        .build();
            }

            int newDuration = DateTimeUtil.calculateDaysBetween(newExpirationDate);
            license.setEndingDate(newExpirationDate);
            license.setDuration(newDuration);

            licenseService.updateLicense(license);
            String resp = String.format("Лицензия с кодом %s продлена до: %s", requestData.getCode(), newExpirationDate);
            log.info(resp);
            Ticket ticket = Ticket.createTicket(license.getOwner().getId(), false, newExpirationDate, resp);

            Optional<DeviceLicense> deviceLicenseOpt = deviceLicenseRepository.findByLicenseId(license.getId());
            Date activationDate = null;
            Long deviceId = null;
            String deviceMessage = null;

            if (deviceLicenseOpt.isPresent()) {
                DeviceLicense deviceLicense = deviceLicenseOpt.get();

                activationDate = deviceLicense.getActivationDate();
                deviceId = deviceLicense.getDevice().getId();

                ticket.setActivationDate(activationDate);
                ticket.setDeviceId(deviceId);
                ticket.setTicketLifetime(newDuration);

                log.info("Тикет: {}", ticket);

                logLicensing(user.getEmail(), user.getUsername(), "Лицензия успешно продлена | " + deviceMessage);
            }

            logLicensing(user.getEmail(), user.getUsername(), "Лицензия успешно продлена");
            return LicenseResponse.builder()
                    .ticket(ticket)
                    .build();

        } catch (ParseException e) {
            log.error("Ошибка при парсинге даты: {}", e.getMessage());
            logLicensing(user.getEmail(), user.getUsername(), "Ошибка обновления лицензии: " + e.getMessage());
            throw e;
        } catch (LicenseException e) {
            log.error("Ошибка: {}", e.getMessage());
            logLicensing(user.getEmail(), user.getUsername(), "Ошибка обновления лицензии: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Произошла ошибка: {}", e.getMessage());
            logLicensing(user.getEmail(), user.getUsername(), "Ошибка обновления лицензии: " + e.getMessage());
            throw new LicenseException("Ошибка обновления лицензии: " + e.getMessage(), true);
        }
    }

    private Device deviceCheck(ApplicationUser user, String macAddress, String deviceName) throws LicenseException {
        var device = deviceService.getDeviceByMacAddressAndName(macAddress, deviceName);
        if (device.isEmpty()) {
            log.error("Ошибка: устройство не найдено с MAC-адресом {} и именем {}", macAddress, deviceName);
            logLicensing(user.getEmail(), user.getUsername(), "Ошибка проверки лицензии: устройство не найдено");
            throw new LicenseException("Устройство не найдено", false, HttpStatus.NOT_FOUND);
        }

        return device.get();
    }

    private DeviceLicense deviceLicenseCheck(ApplicationUser user, Long deviceId) throws LicenseException {
        var deviceLicense = deviceLicenseRepository.findByDeviceId(deviceId);
        if (deviceLicense.isEmpty()) {
            log.warn("Лицензия не найдена для устройства с ID {}", deviceId);
            logLicensing(user.getEmail(), user.getUsername(), String.format("Ошибка проверки лицензии: лицензия не найдена с ID: %d", deviceId));
            throw new LicenseException("Лицензия для устройства не найдена", false, HttpStatus.NOT_FOUND);
        }

        return deviceLicense.get();
    }

    public LicenseResponse checkLicense(ApplicationUser user, LicenseCheckRequest requestData) throws Exception {
        try {
            Device device = deviceCheck(user, requestData.getMacAddress(), requestData.getDeviceName());
            log.info("Устройство найдено: {}", device);

            DeviceLicense deviceLicense = deviceLicenseCheck(user, device.getId());
            Optional<License> licenseOptional = licenseService.getLicenseById(deviceLicense.getLicense().getId());

            if (licenseOptional.isPresent()) {
                logLicensing(user.getEmail(), user.getUsername(), "Успешная проверка лицензии");

                String resp = "Лицензия активирована на устройстве. " + deviceLicense.getLicense().getId();

                License license = licenseOptional.get();
                Ticket ticket = Ticket.createTicket(license.getUser().getId(), false, license.getEndingDate(), resp);
                ticket.setDeviceId(deviceLicense.getDevice().getId());

                log.info("Тикет с подтверждением лицензии: {}", ticket);

                return LicenseResponse.builder().ticket(ticket).build();
            } else {
                log.error("Лицензия с ID {} не найдена", deviceLicense.getLicense().getId());
                Ticket ticket = Ticket.createTicket(null, true, null, "Лицензия не найдена");
                logLicensing(user.getEmail(), user.getUsername(), "Ошибка проверки лицензии: попытка найти несуществующую лицензию");
                return LicenseResponse.builder().ticket(ticket).build();
            }

        } catch (LicenseException e) {
            log.error("Произошла ошибка при проверке лицензии: {}", e.getMessage());
            logLicensing(user.getEmail(), user.getUsername(), "Ошибка проверки лицензии: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Произошла ошибка при проверке лицензии: {}", e.getMessage());
            logLicensing(user.getEmail(), user.getUsername(), "Ошибка проверки лицензии: " + e.getMessage());
            throw new LicenseException("Ошибка проверки: " + e.getMessage(), true);
        }
    }

    private License licenseCheck(String licenseCode, ApplicationUser userRequest, boolean createTicket) throws LicenseException {
        var license = licenseCheck(licenseCode, createTicket);

        if (license.getUser() != null) {
            if (!license.getUser().getEmail().equals(userRequest.getEmail())) {
                logLicensing(userRequest.getEmail(), userRequest.getUsername(), "Ошибка активации лицензии: некорректный email");
                throw new LicenseException("Ошибка", false);
            }
        }

        return license;
    }

    public synchronized LicenseResponse activateLicense(ApplicationUser user, LicenseActivationRequest activationRequest) throws Exception {
        try {
            License license = licenseCheck(activationRequest.getCode(), user, false);

            if (license.getFirstActivationDate() == null) {
                license.setFirstActivationDate(DateTimeUtil.convertLocalDateToDate(LocalDate.now()));
                int duration = license.getLicenseType().getDefaultDuration();
                LocalDate endingLocalDate = LocalDate.now().plusDays(duration);
                license.setEndingDate(DateTimeUtil.convertLocalDateToDate(endingLocalDate));
                license.setUser(user);
            }

            Optional<Device> existingDevice = deviceService.getDeviceByMacAddressAndName(activationRequest.getMacAddress(), activationRequest.getDeviceName());
            if (existingDevice.isPresent()) {
                if (deviceService.isMacAddressExists(activationRequest.getMacAddress()) && deviceService.isDeviceExists(activationRequest.getMacAddress(), activationRequest.getDeviceName())) {
                    log.error("Устройство с MAC-адресом {} и именем {} уже существует", activationRequest.getMacAddress(), activationRequest.getDeviceName());
                    logLicensing(user.getEmail(), user.getUsername(), "Ошибка активации лицензии: существующий MAC-address и deviceName");
                    throw new LicenseException("Устройство с таким MAC-адресом и именем уже существует");
                } else if (deviceService.isMacAddressExists(activationRequest.getMacAddress())) {
                    log.error("Устройство с MAC-адресом {} уже существует", activationRequest.getMacAddress());
                    logLicensing(user.getEmail(), user.getUsername(), "Ошибка активации лицензии: существующий MAC-address");
                    throw new LicenseException("Устройство с таким MAC-адресом и именем уже существует");
                }
            }

            Optional<Device> deviceOptional = deviceService.getDeviceByMacAddressAndName(activationRequest.getMacAddress(), activationRequest.getDeviceName());
            Device device;

            if (existingDevice.isPresent()) {
                device = deviceOptional.get();
                log.info("Устройство найдено: {}", device);

            } else {
                device = new Device();
                device.setMacAddress(activationRequest.getMacAddress());
                if (deviceService.isMacAddressExists(activationRequest.getMacAddress())) {
                    logLicensing(user.getEmail(), user.getUsername(), "Ошибка активации лицензии: существующий MAC-address");
                    throw new LicenseException("Устройство с таким MAC-адресом уже существует");
                }
                device.setName(activationRequest.getDeviceName());
                device.setUserId(license.getUser().getId());
                deviceService.saveDevice(device);
                logLicensing(user.getEmail(), user.getUsername(), "Добавление новых устройств в лицензию");
                log.info("Устройство с MAC-адресом {} и именем {} зарегистрировано", activationRequest.getMacAddress(), activationRequest.getDeviceName());
            }


            if (license.getDeviceCount() <= 0) {
                log.warn("Для лицензии с кодом {} нет доступных мест для активации", activationRequest.getCode());
                logLicensing(user.getEmail(), user.getUsername(), "Ошибка активации лицензии: превышен лимит количества устройств на лицензии");
                throw new LicenseException("Превышен лимит количества устройств на лицензии");
            }

            Optional<DeviceLicense> existingDeviceLicenseOptional = deviceLicenseRepository.findByDeviceIdAndLicenseId(device.getId(), license.getId());

            if (existingDeviceLicenseOptional.isPresent()) {
                logLicensing(user.getEmail(), user.getUsername(), "Ошибка активации лицензии: уже имеется активная лицензия на данном устройстве");
                log.warn("Лицензия {} уже активирована на устройстве с ID {}", activationRequest.getCode(), device.getId());
                throw new LicenseException("Лицензия уже активирована на данном устройстве");
            }

            DeviceLicense deviceLicense = new DeviceLicense();
            deviceLicense.setLicense(license);
            deviceLicense.setDevice(device);
            deviceLicense.setActivationDate(new Date());
            deviceLicenseRepository.save(deviceLicense);
            log.info("Лицензия {} активирована на устройстве с ID {}", activationRequest.getCode(), device.getId());

            license.setDeviceCount(license.getDeviceCount() - 1);
            licenseService.updateLicense(license);
            log.info("Количество доступных мест для активации на лицензии с кодом {} уменьшено на 1", activationRequest.getCode());

            String description = "Лицензия активирована на устройстве " + device.getName();
            Date changeDate = new Date();
            licenseHistoryService.recordLicenseChange(license.getId(), license.getUser().getId(), "Активирована", changeDate, description);
            log.info("Запись изменений лицензии в историю завершена");
            logLicensing(user.getEmail(), user.getUsername(), "Успешная активация лицензии с ID: " + license.getId());

            var detail = String.format("Лицензия успешно активирована на устройстве [%s:%s] | Дата начала лицензии %s | Дата окончания лицензии: %s",
                    device.getName(),
                    device.getMacAddress(),
                    license.getFirstActivationDate(),
                    license.getEndingDate());

            Ticket ticket = Ticket.createTicket(null, false, license.getEndingDate(), detail);
            log.info("Тикет с подтверждением активации лицензии: {}", ticket);

            return LicenseResponse.builder()
                    .ticket(ticket)
                    .build();

        } catch (LicenseException e) {
            throw e;
        } catch (Exception e) {
            log.error("Произошла ошибка при активации: {}", e.getMessage(), e);
            logLicensing(user.getEmail(), user.getUsername(), "Ошибка активации: " + e.getMessage());
            throw new LicenseException("Ошибка активации: " + e.getMessage(), true);
        }
    }
}
