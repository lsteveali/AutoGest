package com.autogest.util;

import javafx.application.Platform;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Ejecuta trabajo lento (consultas a la base de datos, cifrado de contraseñas)
 * fuera del hilo de JavaFX y entrega el resultado de vuelta en ese hilo, para
 * que la ventana no se congele mientras espera.
 *
 * <p>Los hilos son de tipo daemon: no impiden cerrar la aplicación.</p>
 *
 * <p>Principles: Pure Fabrication, Indirection.</p>
 */
public final class Background {

    private static final AtomicInteger THREAD_COUNT = new AtomicInteger();

    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool(task -> {
        Thread thread = new Thread(task, "autogest-bg-" + THREAD_COUNT.incrementAndGet());
        thread.setDaemon(true);
        return thread;
    });

    private Background() {
    }

    /**
     * Ejecuta {@code work} en segundo plano y llama {@code onSuccess} o
     * {@code onError} en el hilo de JavaFX.
     *
     * @param work      trabajo a ejecutar fuera del hilo de la interfaz
     * @param onSuccess recibe el resultado, en el hilo de JavaFX
     * @param onError   recibe el error lanzado, en el hilo de JavaFX
     * @param <T>       tipo del resultado
     */
    public static <T> void run(Supplier<T> work, Consumer<T> onSuccess, Consumer<RuntimeException> onError) {
        EXECUTOR.execute(() -> {
            try {
                T result = work.get();
                Platform.runLater(() -> onSuccess.accept(result));
            } catch (RuntimeException e) {
                Platform.runLater(() -> onError.accept(e));
            }
        });
    }
}
