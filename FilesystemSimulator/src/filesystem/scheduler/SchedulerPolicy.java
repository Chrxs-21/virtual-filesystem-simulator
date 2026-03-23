/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package filesystem.scheduler;

/**
 * Políticas de planificación de disco disponibles.
 */
public enum SchedulerPolicy {
    FIFO,
    SSTF,
    SCAN,
    C_SCAN
}
