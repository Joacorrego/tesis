package com.pruebaapi.app.Controllers;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserRecord;
import com.google.firebase.auth.UserRecord.CreateRequest;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.ListUsersPage;
import com.google.firebase.auth.ExportedUserRecord;
import com.google.firebase.cloud.FirestoreClient;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.DocumentSnapshot;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.concurrent.ConcurrentHashMap;
import java.util.*;
import java.util.concurrent.ExecutionException;

import com.pruebaapi.Model.Usuario;
import com.pruebaapi.Model.LoginCredentials;
import com.pruebaapi.Model.Tablero;
import com.pruebaapi.Model.Partida;
import com.pruebaapi.Utils.CalcularTablero;

@RestController
@RequestMapping("/usuarios")
public class Controller {

    private static final Logger logger = LoggerFactory.getLogger(Controller.class);

    private ConcurrentHashMap<Long, Usuario> usuarios = new ConcurrentHashMap<>();
    private Long idSecuencia = 1L;

    @PostMapping("/registrar")
    public ResponseEntity<?> registrarUsuario(@RequestBody Usuario usuario) {
        UserRecord userRecord = null;
        try {
            // Verificar si el usuario ya existe en Firebase Authentication
            if (emailExistsInFirebase(usuario.getEmail())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("El correo electrónico ya está registrado.");
            }

            CreateRequest request = new CreateRequest()
                    .setEmail(usuario.getEmail())
                    .setPassword(usuario.getPassword())
                    .setEmailVerified(false)
                    .setDisplayName(usuario.getNombre())
                    .setDisabled(false);

            userRecord = FirebaseAuth.getInstance().createUser(request);
            usuario.setFirebaseUid(userRecord.getUid());

            Firestore db = FirestoreClient.getFirestore();
            DocumentReference docRef = db.collection("usuarios").document(userRecord.getUid());
            Map<String, Object> userData = new HashMap<>();
            userData.put("nombre", usuario.getNombre());
            userData.put("email", usuario.getEmail());
            docRef.set(userData).get();

            // Crear tablero para el usuario
            String idTablero = UUID.randomUUID().toString();
            Tablero nuevoTablero = new Tablero();
            nuevoTablero.setIdTablero(idTablero);
            nuevoTablero.setUsuario(usuario);

            // Inicializar casillas
            int[][] casillas = new int[5][5];
            nuevoTablero.setCasillasFromArray(casillas);

            DocumentReference tableroRef = db.collection("tableros").document(idTablero);
            Map<String, Object> tableroData = new HashMap<>();
            tableroData.put("idTablero", nuevoTablero.getIdTablero());
            tableroData.put("usuario", docRef);
            tableroData.put("casillas", nuevoTablero.getCasillas());
            tableroRef.set(tableroData).get();

            usuario.setIdTablero(idTablero); // Guardar el idTablero en el usuario

            usuario.setId(idSecuencia);
            usuarios.put(idSecuencia, usuario);
            idSecuencia++;

            return ResponseEntity.ok(usuario);
        } catch (FirebaseAuthException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error de Firebase Authentication: " + e.getMessage());
        } catch (Exception e) {
            if (userRecord != null) {
                try {
                    FirebaseAuth.getInstance().deleteUser(userRecord.getUid());
                } catch (FirebaseAuthException ex) {
                }
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al registrar el usuario: " + e.getMessage());
        }
    }

    private boolean emailExistsInFirebase(String email) {
        try {
            ListUsersPage page = FirebaseAuth.getInstance().listUsers(null);
            for (ExportedUserRecord user : page.iterateAll()) {
                if (user.getEmail().equals(email)) {
                    return true;
                }
            }
        } catch (FirebaseAuthException e) {
        }
        return false;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginCredentials credentials) {
        try {
            UserRecord userRecord = FirebaseAuth.getInstance().getUserByEmail(credentials.getEmail());

            if (userRecord != null) {
                Map<String, Object> response = new HashMap<>();
                response.put("message", "Bienvenido " + credentials.getEmail());
                response.put("firebaseUid", userRecord.getUid());

                Firestore db = FirestoreClient.getFirestore();
                CollectionReference tablerosRef = db.collection("tableros");
                Query query = tablerosRef.whereEqualTo("usuario", db.collection("usuarios").document(userRecord.getUid()));
                ApiFuture<QuerySnapshot> querySnapshot = query.get();
                List<QueryDocumentSnapshot> documents = querySnapshot.get().getDocuments();

                if (!documents.isEmpty()) {
                    QueryDocumentSnapshot tableroDoc = documents.get(0);
                    response.put("idTablero", tableroDoc.getId());
                    response.put("tableroData", tableroDoc.getData());  // Agrega más datos si es necesario para depuración
                } else {
                    logger.warn("No se encontró idTablero para el usuario: {}", userRecord.getUid());
                    response.put("idTablero", null);
                }

                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Credenciales inválidas");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al iniciar sesión: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public Usuario obtenerUsuario(@PathVariable Long id) {
        return usuarios.get(id);
    }

    @GetMapping("/todos")
    public Collection<Usuario> obtenerTodosLosUsuarios() {
        return usuarios.values();
    }

    @PostMapping("/tableros/registrar")
    public ResponseEntity<?> registrarTablero(@RequestBody Tablero tablero) {
        try {
            Firestore db = FirestoreClient.getFirestore();
            String idTablero = UUID.randomUUID().toString();
            tablero.setIdTablero(idTablero);

            String emailUsuario = tablero.getUsuario().getEmail();
            CollectionReference usuariosRef = db.collection("usuarios");
            Query query = usuariosRef.whereEqualTo("email", emailUsuario);
            ApiFuture<QuerySnapshot> querySnapshot = query.get();
            QueryDocumentSnapshot document = querySnapshot.get().getDocuments().get(0);
            DocumentReference usuarioRef = document.getReference();

            DocumentReference tableroRef = db.collection("tableros").document(idTablero);
            Map<String, Object> tableroData = new HashMap<>();
            tableroData.put("idTablero", tablero.getIdTablero());
            tableroData.put("usuario", usuarioRef);
            tableroData.put("casillas", tablero.getCasillas());
            tableroRef.set(tableroData);

            return ResponseEntity.ok(tablero);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al registrar el tablero: " + e.getMessage());
        }
    }

    @PostMapping("/partidas/crear")
    public ResponseEntity<?> crearPartida(@RequestBody Map<String, String> request) {
        try {
            String firebaseUid = request.get("firebaseUid");
            String idTablero = request.get("idTablero");

            Firestore db = FirestoreClient.getFirestore();
            String idPartida = UUID.randomUUID().toString();
            String codigoPartida = generarCodigoPartida();

            Partida nuevaPartida = new Partida();
            nuevaPartida.setCodigoPartida(codigoPartida);
            nuevaPartida.setJugador1(new Usuario(firebaseUid));
            nuevaPartida.setTablero1(new Tablero(idTablero));

            DocumentReference partidaRef = db.collection("partidas").document(idPartida);
            Map<String, Object> partidaData = new HashMap<>();
            partidaData.put("idPartida", idPartida);
            partidaData.put("codigoPartida", codigoPartida);
            partidaData.put("jugador1", db.collection("usuarios").document(firebaseUid));
            partidaData.put("tablero1", db.collection("tableros").document(idTablero));
            partidaData.put("fecha", nuevaPartida.getFecha());
            partidaData.put("puntuacionJ1", nuevaPartida.getPuntuacionJ1());
            partidaData.put("puntuacionJ2", nuevaPartida.getPuntuacionJ2());
            partidaData.put("turno", nuevaPartida.getTurno());
            partidaRef.set(partidaData);

            return ResponseEntity.ok(codigoPartida);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al crear la partida: " + e.getMessage());
        }
    }

    @PostMapping("/partidas/registrar")
    public ResponseEntity<?> registrarPartida(@RequestBody Map<String, String> request) {
        try {
            String codigoPartida = request.get("codigoPartida");
            String firebaseUid = request.get("firebaseUid");
            String idTablero = request.get("idTablero");

            Firestore db = FirestoreClient.getFirestore();

            Query query = db.collection("partidas").whereEqualTo("codigoPartida", codigoPartida);
            ApiFuture<QuerySnapshot> querySnapshot = query.get();
            QueryDocumentSnapshot partidaDoc = null;

            for (QueryDocumentSnapshot document : querySnapshot.get().getDocuments()) {
                partidaDoc = document;
                break;
            }

            if (partidaDoc == null) {
                logger.warn("Partida no encontrada: {}", codigoPartida);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Partida no encontrada.");
            }

            DocumentReference partidaRef = db.collection("partidas").document(partidaDoc.getId());
            partidaRef.update("jugador2", db.collection("usuarios").document(firebaseUid));
            partidaRef.update("tablero2", db.collection("tableros").document(idTablero));

            return ResponseEntity.ok("Partida actualizada correctamente");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al registrar la partida: " + e.getMessage());
        }
    }

    @PatchMapping("/partidas/{idPartida}/actualizar-tablero/{idTablero}")
    public ResponseEntity<?> actualizarCasillasTablero(
        @PathVariable String idPartida,
        @PathVariable String idTablero,
        @RequestBody int[][] nuevasCasillas) {
        try {
            Firestore db = FirestoreClient.getFirestore();

            logger.info("Recibida solicitud para actualizar tablero con idPartida: {} y idTablero: {}", idPartida, idTablero);

            DocumentReference partidaRef = db.collection("partidas").document(idPartida);
            DocumentSnapshot partidaDoc = partidaRef.get().get();
            if (!partidaDoc.exists()) {
                logger.warn("Partida no encontrada: {}", idPartida);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Partida no encontrada.");
            }

            Map<String, Object> partidaData = partidaDoc.getData();
            if (partidaData == null) {
                logger.warn("Datos de la partida no encontrados para idPartida: {}", idPartida);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Datos de la partida no encontrados.");
            }

            int turnoActual = partidaDoc.getLong("turno").intValue();
            String idTableroObjetivo;
            String jugadorASumar;

            if (turnoActual == 1) {
                idTableroObjetivo = ((DocumentReference) partidaData.get("tablero2")).getId();
                jugadorASumar = "puntuacionJ1";
            } else if (turnoActual == 2) {
                idTableroObjetivo = ((DocumentReference) partidaData.get("tablero1")).getId();
                jugadorASumar = "puntuacionJ2";
            } else {
                return ResponseEntity.badRequest().body("Turno inválido.");
            }

            if (!idTablero.equals(idTableroObjetivo)) {
                return ResponseEntity.badRequest().body("No es el turno de este tablero.");
            }

            DocumentReference tableroRef = db.collection("tableros").document(idTableroObjetivo);
            DocumentSnapshot tableroDoc = tableroRef.get().get();

            if (!tableroDoc.exists()) {
                logger.warn("Tablero no encontrado: {}", idTableroObjetivo);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Tablero no encontrado.");
            }

            String casillasActualesJson = tableroDoc.getString("casillas");
            Type listType = new TypeToken<int[][]>() {}.getType();
            int[][] casillasActuales = new Gson().fromJson(casillasActualesJson, listType);

            int[][] resultado;
            try {
                resultado = CalcularTablero.sumarTableros(casillasActuales, nuevasCasillas);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(e.getMessage());
            }

            String resultadoJson = new Gson().toJson(resultado);

            tableroRef.update("casillas", resultadoJson);
            int puntosASumar = CalcularTablero.calcularPuntuacion(nuevasCasillas, casillasActuales);
            int puntuacionActualizada = partidaDoc.getLong(jugadorASumar).intValue() + puntosASumar;
            boolean disparoExitoso = puntosASumar > 0;

            String mensaje;
            if (disparoExitoso) {
                mensaje = "Disparo exitoso, Puedes seguir disparando";
            } else {
                mensaje = "Disparo fallido, Turno del siguiente jugador";
                turnoActual = (turnoActual == 1) ? 2 : 1;
            }

            Map<String, Object> updates = new HashMap<>();
            updates.put(jugadorASumar, puntuacionActualizada);
            updates.put("turno", turnoActual);
            partidaRef.update(updates).get();

            // Obtener puntajes actualizados
            partidaDoc = partidaRef.get().get();
            int puntuacionJ1 = partidaDoc.getLong("puntuacionJ1").intValue();
            int puntuacionJ2 = partidaDoc.getLong("puntuacionJ2").intValue();

            if (puntuacionJ1 >= 8 || puntuacionJ2 >= 8) {
                String ganador;
                if (puntuacionJ1 >= 8) {
                    ganador = partidaData.get("jugador1").toString();
                } else {
                    ganador = partidaData.get("jugador2").toString();
                }

                Map<String, Object> resultadoFinal = new HashMap<>();
                resultadoFinal.put("ganador", ganador);
                resultadoFinal.put("puntuacionJ1", puntuacionJ1);
                resultadoFinal.put("puntuacionJ2", puntuacionJ2);

                mensaje = "¡Fin del juego! El ganador es " + ganador + ".";
                resultadoFinal.put("mensaje", mensaje);

                return ResponseEntity.ok(resultadoFinal);
            }

            DocumentReference tablero1Ref = db.collection("tableros").document(((DocumentReference) partidaData.get("tablero1")).getId());
            DocumentReference tablero2Ref = db.collection("tableros").document(((DocumentReference) partidaData.get("tablero2")).getId());
            DocumentSnapshot tablero1Doc = tablero1Ref.get().get();
            DocumentSnapshot tablero2Doc = tablero2Ref.get().get();

            String tablero1Json = tablero1Doc.getString("casillas");
            String tablero2Json = tablero2Doc.getString("casillas");

            // Respuesta después de cada jugada
            Map<String, Object> respuesta = new HashMap<>();
            respuesta.put("mensaje", mensaje);
            respuesta.put("puntuacionJ1", puntuacionJ1);
            respuesta.put("puntuacionJ2", puntuacionJ2);
            respuesta.put("tablero1", tablero1Json);
            respuesta.put("tablero2", tablero2Json);
            respuesta.put("ultimoDisparo", nuevasCasillas);
            respuesta.put("turnoActual", turnoActual);

            return ResponseEntity.ok(respuesta);
        } catch (Exception e) {
            logger.error("Error al actualizar las casillas del tablero: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al actualizar las casillas del tablero: " + e.getMessage());
        }
    }

    @GetMapping("/partidas/id/{codigoPartida}")
    public ResponseEntity<String> obtenerIdPartida(@PathVariable String codigoPartida) {
        try {
            Firestore db = FirestoreClient.getFirestore();
            Query query = db.collection("partidas").whereEqualTo("codigoPartida", codigoPartida);
            ApiFuture<QuerySnapshot> querySnapshot = query.get();
            QueryDocumentSnapshot partidaDoc = querySnapshot.get().getDocuments().get(0);

            if (partidaDoc.exists()) {
                return ResponseEntity.ok(partidaDoc.getId());
            } else {
                logger.warn("Partida no encontrada: {}", codigoPartida);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Partida no encontrada.");
            }
        } catch (Exception e) {
            logger.error("Error al obtener el ID de la partida: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al obtener el ID de la partida: " + e.getMessage());
        }
    }

    @GetMapping("/partidas/{idPartida}/puntajes")
    public ResponseEntity<Map<String, Integer>> obtenerPuntajes(@PathVariable String idPartida) {
        try {
            Firestore db = FirestoreClient.getFirestore();
            DocumentReference partidaRef = db.collection("partidas").document(idPartida);
            DocumentSnapshot partidaDoc = partidaRef.get().get();

            if (!partidaDoc.exists()) {
                logger.warn("Partida no encontrada: {}", idPartida);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }

            int puntaje1 = partidaDoc.getLong("puntuacionJ1").intValue();
            int puntaje2 = partidaDoc.getLong("puntuacionJ2").intValue();

            Map<String, Integer> puntajes = new HashMap<>();
            puntajes.put("puntaje1", puntaje1);
            puntajes.put("puntaje2", puntaje2);

            return ResponseEntity.ok(puntajes);
        } catch (Exception e) {
            logger.error("Error al obtener los puntajes: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    private String generarCodigoPartida() {
        String codigo = UUID.randomUUID().toString().substring(0, 8);
        return codigo;
    }
}
