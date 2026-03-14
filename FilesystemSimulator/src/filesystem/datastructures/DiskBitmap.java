/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package filesystem.datastructures;

/**
 * Bitmap del disco. Gestiona bloques libres y ocupados
 * usando un arreglo primitivo boolean[].
 */
public class DiskBitmap {
    private final boolean[] blocks;
    private final int totalBlocks;
    private int freeCount;

    public DiskBitmap(int totalBlocks) {
        this.totalBlocks = totalBlocks;
        this.blocks = new boolean[totalBlocks];
        this.freeCount = totalBlocks;
        for (int i = 0; i < totalBlocks; i++) blocks[i] = true;
    }

    // Métodos se implementarán en el Hito 1
}
