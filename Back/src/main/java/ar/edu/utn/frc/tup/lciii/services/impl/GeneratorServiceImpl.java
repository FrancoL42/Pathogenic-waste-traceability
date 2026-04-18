package ar.edu.utn.frc.tup.lciii.services.impl;

import ar.edu.utn.frc.tup.lciii.dtos.GeneratorDto;
import ar.edu.utn.frc.tup.lciii.dtos.UserDTO;
import ar.edu.utn.frc.tup.lciii.entities.GeneratorEntity;
import ar.edu.utn.frc.tup.lciii.entities.UserEntity;
import ar.edu.utn.frc.tup.lciii.entities.ZoneEntity;
import ar.edu.utn.frc.tup.lciii.models.Generator;
import ar.edu.utn.frc.tup.lciii.models.RegisterState;
import ar.edu.utn.frc.tup.lciii.models.Role;
import ar.edu.utn.frc.tup.lciii.repositories.GeneratorRepository;
import ar.edu.utn.frc.tup.lciii.repositories.ZoneRepository;
import ar.edu.utn.frc.tup.lciii.services.ContractService;
import ar.edu.utn.frc.tup.lciii.services.GeneratorService;
import ar.edu.utn.frc.tup.lciii.services.GeocodingService;
import ar.edu.utn.frc.tup.lciii.services.UserService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class GeneratorServiceImpl implements GeneratorService {
    @Autowired
    private GeneratorRepository repository;
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private EnvioCorreosServiceImpl emailService;
    @Autowired
    private GeocodingService geocodingService;
    @Autowired
    private UserService userService;
    @Autowired
    private ContractService contractService;
    @Autowired
    private ZoneRepository zoneRepository;
    public Generator registrationGenerator(GeneratorDto o) {
        GeneratorEntity generatorSave = modelMapper.map(o, GeneratorEntity.class);
        if (o.getZona() != null && !o.getZona().isEmpty()) {
            ZoneEntity zone = zoneRepository.findByName(o.getZona());
            if (zone != null) {
                generatorSave.setZone(zone);
            } else {
                throw new IllegalArgumentException("Zona no encontrada: " + o.getZona());
            }
        }
        generatorSave.setState(RegisterState.PENDIENTE);
        generatorSave.setType(o.getType());
        generatorSave.setContact(o.getContact());
        generatorSave.setLatitude(o.getLatitude());
        generatorSave.setLongitude(o.getLongitude());

        if (o.getLatitude() != null && o.getLongitude() != null) {
            // Si vienen coordenadas del frontend (GPS), usarlas directamente
            generatorSave.setLatitude(o.getLatitude());
            generatorSave.setLongitude(o.getLongitude());

            System.out.println("✅ Coordenadas GPS del frontend: " +
                    o.getLatitude() + ", " + o.getLongitude());

            // Verificar que estén dentro de Córdoba
            if (isWithinCordoba(o.getLatitude(), o.getLongitude())) {
                System.out.println("🎯 Coordenadas GPS válidas dentro de Córdoba");
            } else {
                System.out.println("⚠️ Coordenadas GPS fuera de Córdoba, usando geocodificación");
                // Si están fuera de Córdoba, geocodificar la dirección como fallback
                geocodificarDireccion(generatorSave);
            }
        }
        GeneratorEntity generatorEntity = repository.save(generatorSave);
        Generator generator = modelMapper.map(generatorEntity, Generator.class);
        if (generatorEntity.getZone() != null) {
            generator.setZona(generatorEntity.getZone().getName());
        }
        // Generar contrato en PDF
        byte[] contratosPdf = contractService.generarContratoComercial(generator);

        // Preparar el cuerpo del correo
        String cuerpoCorreo = "Estimado/a " + generator.getName() + ",\n\n" +
                "Se ha recibido exitosamente su solicitud de registro al Sistema de Gestión de Residuos Patológicos " +
                "de AESA Misiones S.A.\n\n" +
                "DOCUMENTACIÓN ADJUNTA:\n" +
                "📄 Acuerdo Comercial de Provisión de Bolsas\n\n" +
                "PASOS A SEGUIR:\n" +
                "1. Revisar detenidamente el Acuerdo Comercial adjunto\n" +
                "2. Completar los datos faltantes en el Anexo II\n" +
                "3. Firmar el documento en los espacios correspondientes\n" +
                "4. Adjuntar la documentación requerida según su categoría:\n\n" +
                "📋 PERSONAS FÍSICAS:\n" +
                "• Fotocopia de DNI\n" +
                "• Constancia de CUIT vigente (firmada por el titular)\n" +
                "• Certificados de no percepción y/o exención en IIBB\n" +
                "• Constancia de inscripción en IIBB\n" +
                "• Documento que certifique su profesión\n" +
                "• Habilitación municipal correspondiente\n\n" +
                "🏢 PERSONAS JURÍDICAS:\n" +
                "• Constancia de CUIT vigente (firmada por el responsable)\n" +
                "• Constancia de inscripción en AFIP\n" +
                "• Constancia de Agente de Recaudación\n" +
                "• Certificados de no percepción y/o exención en IIBB\n" +
                "• Poder que habilite la contratación (si es sociedad)\n" +
                "• Constancia de inscripción en IIBB\n" +
                "• Habilitación municipal correspondiente\n\n" +
                "📧 ENVÍO DE DOCUMENTACIÓN:\n" +
                "Presentar toda la documentación en el centro más cercano o " +
                "enviar toda la documentación a: recepcion.misiones@veolia.com\n" +
                "Asunto: \"Documentación - " + generator.getName() + "\"\n\n" +
                "📞 CONTACTO:\n" +
                "• Email: recepcion.misiones@veolia.com\n" +
                "• Teléfono: +54 9 376 - 4108604\n" +
                "• WhatsApp: +54 9 376 - 5172011\n\n" +
                "Agradecemos su interés en nuestros servicios.\n\n" +
                "Saludos cordiales,\n" +
                "Equipo de Atención al Cliente\n" +
                "AESA Misiones S.A.";

        // Enviar correo con contrato adjunto
        try {
            emailService.enviarCorreoConAdjunto(
                    o.getEmail(),
                    "Solicitud de Registro - Acuerdo Comercial Adjunto",
                    cuerpoCorreo,
                    "Acuerdo_Comercial_" + generator.getName().replaceAll("\\s+", "_") + ".pdf",
                    contratosPdf
            );
        } catch (Exception e) {
            // Log del error pero no fallar el registro
            System.err.println("Error enviando correo con contrato: " + e.getMessage());
            // Enviar correo simple como fallback
            emailService.enviarCorreo(o.getEmail(), "Solicitud de Registro Recibida",
                    "Su solicitud ha sido recibida. Nos contactaremos pronto con la documentación necesaria.");
        }

        return generator;
    }
    private void geocodificarDireccion(GeneratorEntity generatorSave) {
        String direccionCompleta = generatorSave.getAddress();
        Optional<double[]> coords = geocodingService.obtenerCoordenadasDesdeDireccion(direccionCompleta);

        if (coords.isPresent()) {
            double[] latLng = coords.get();
            generatorSave.setLatitude(latLng[0]);
            generatorSave.setLongitude(latLng[1]);
            System.out.println("✅ Geocodificación exitosa: " + latLng[0] + ", " + latLng[1]);
        } else {
            // Fallback: Centro de Córdoba
            generatorSave.setLatitude(-31.4201);
            generatorSave.setLongitude(-64.1888);
            System.out.println("🏠 Usando coordenadas del centro de Córdoba como fallback");
        }
    }

    // 🆕 MÉTODO AUXILIAR PARA VALIDAR CÓRDOBA
    private boolean isWithinCordoba(Double lat, Double lon) {
        if (lat == null || lon == null) return false;

        final double CORDOBA_MIN_LAT = -31.50;
        final double CORDOBA_MAX_LAT = -31.30;
        final double CORDOBA_MIN_LON = -64.30;
        final double CORDOBA_MAX_LON = -64.05;

        return lat >= CORDOBA_MIN_LAT && lat <= CORDOBA_MAX_LAT &&
                lon >= CORDOBA_MIN_LON && lon <= CORDOBA_MAX_LON;
    }
    @Override
    public Generator withdrawalGenerator(String name) {
        GeneratorEntity generatorWithdrawal = repository.getGeneratorEntityByName(name);
        //TODO: Validaciones
        generatorWithdrawal.setState(RegisterState.PENDIENTE_BAJA);
        repository.save(generatorWithdrawal);
        return modelMapper.map(generatorWithdrawal, Generator.class);
    }

    @Override
    public Generator aproveWithdrawalGenerator(Boolean bool, Long id) {
        Optional<GeneratorEntity> generator = repository.findById(id);
        //TODO: VALIDACIONES
        if(generator.isEmpty()) {
            throw new IllegalArgumentException("No se encontró la solicitud");
        }
        if (!bool) {
            generator.get().setState(RegisterState.ACTIVO);
        } else {
            generator.get().setState(RegisterState.INACTIVO);
            generator.get().setExitDate(LocalDateTime.now());
        }
        GeneratorEntity generatorEntity = repository.save(generator.get());
        //noTodo: Aca mandar correo
        emailService.enviarCorreo(generatorEntity.getEmail(),"Baja", "Se ha aprobado su solicitud de baja al servicio");
        return modelMapper.map(generatorEntity, Generator.class);
    }

    @Override
    public Generator aproveGenerator(Boolean bool, Long id) {
        Optional<GeneratorEntity> generator = repository.findById(id);
        if(generator.isEmpty()) {
            throw new IllegalArgumentException("No se encontró la solicitud");
        }


        // Generar password aleatoria
        String generatedPassword = generateRandomPassword();
        UserDTO userDTO = new UserDTO();
        userDTO.setRole(Role.CLIENTE);
        userDTO.setEmail(generator.get().getEmail());
        userDTO.setPassword(generatedPassword);

        // Registrar usuario y obtener el usuario guardado (con password codificada)
        UserDTO savedUserDTO = userService.registerUser(userDTO);

        // Mapear el usuario YA GUARDADO (con password codificada)
        generator.get().setUser(modelMapper.map(savedUserDTO, UserEntity.class));
        generator.get().setState(RegisterState.ACTIVO);
        generator.get().setEntryDate(LocalDateTime.now());
        GeneratorEntity generatorEntity = repository.save(generator.get());

        // Aca mandar correo
        emailService.enviarCorreo(generatorEntity.getEmail(),"Registro", "Se ha aprobado su solicitud de adhesión al servicio" +
                "Se a creado el usuario para el sistema de Veolia \n" +
                "Usuario: " + generator.get().getEmail() + "\n"+
                "Contraseña: " + generatedPassword);
        return modelMapper.map(generatorEntity, Generator.class);
    }
    private String generateRandomPassword() {
        String upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lower = "abcdefghijklmnopqrstuvwxyz";
        String allChars = upper + lower;
        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder();

        for (int i = 0; i < 8; i++) {
            int index = random.nextInt(allChars.length());
            password.append(allChars.charAt(index));
        }

        return password.toString();
    }
    @Override
    public List<Generator> getAllACtiveGenerator() {
        List<GeneratorEntity> generatorEntities = repository.getGeneratorEntityByState(RegisterState.ACTIVO);
        List<Generator> generators = new ArrayList<>();
        for (GeneratorEntity generatorEntity : generatorEntities) {
            generators.add(modelMapper.map(generatorEntity, Generator.class));
        }
        return generators;
    }

    @Override
    public List<Generator> getAllPendingGenerator() {
        List<GeneratorEntity> generatorEntities = repository.getGeneratorEntityByState(RegisterState.PENDIENTE);
        List<Generator> generators = new ArrayList<>();
        for (GeneratorEntity generatorEntity : generatorEntities) {
            generators.add(modelMapper.map(generatorEntity, Generator.class));
        }
        return generators;
    }
}
