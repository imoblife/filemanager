package com.filemanager.occupancy;

import android.os.StatFs;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by wuhao on 2015/6/15.
 */
public class FileTreeNode<T> implements Iterable<T> {

    //data use for file
    public File data;
    //the file size
    public long size = 0;

    FileTreeNode<T> parent = null;
    ArrayList<FileTreeNode<T>> children;

    public FileTreeNode(File data) {
        this.data = data;
        this.children = new ArrayList<FileTreeNode<T>>();
        initSize();
    }

    private void initSize() {
        if (data.isFile()) {
            size = data.length();
        } else {
            StatFs fs = new StatFs(data.getPath());
            size = fs.getBlockSize();
        }
    }

    public FileTreeNode<T> addChild(File child) {
        FileTreeNode<T> childNode = new FileTreeNode<T>(child);
        childNode.parent = this;
        size = size + childNode.size;
        this.children.add(childNode);
        return childNode;
    }

    @Override
    public Iterator<T> iterator() {
        //add something
        return null;
    }

    public final void refresh() {
        initSize();
        Iterator iterator = children.iterator();
        while (iterator.hasNext()) {
            FileTreeNode<T> tmp = (FileTreeNode<T>) iterator.next();
            size += tmp.size;
        }
    }
}

