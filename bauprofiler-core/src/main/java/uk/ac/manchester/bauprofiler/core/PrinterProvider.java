/*
 * This file is part of Tornado: A heterogeneous programming framework:
 * https://github.com/beehive-lab/tornado
 *
 * Copyright (c) 2013-2019, APT Group, School of Computer Science,
 * The University of Manchester. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Authors: Benjamin Bell
 *
 */
package uk.ac.manchester.bauprofiler.core;

import java.io.PrintWriter;
import java.io.FileWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;

import uk.ac.manchester.bauprofiler.core.assembler.Assembly;

public class PrinterProvider {
    private static PrintWriter out = null;

    static {
        if (Profiler.SERVER_OUTPUT) {
            try {
                ServerSocket serverSocket = new ServerSocket(Profiler.SERVER_PORT);
                Socket clientSocket = serverSocket.accept(); // blocks execution
                out = new PrintWriter(clientSocket.getOutputStream(), true);
            } catch (Exception e) {
                throw new ServerOutputException(e.getMessage());
            }
        } else if (Profiler.FILE_OUTPUT) {
            try {
                FileWriter fw = new FileWriter(Profiler.FILE_PATH, true);
                out = new PrintWriter(fw, true);
            } catch (IOException e) {
                throw new FileOutputException(e.getMessage());
            }
        } else
            out = new PrintWriter(System.out, true);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                out.close();
            }
        });
    }

    public static ProfilerPrinter printer() {
        return new ProfilerPrinter() {
            public void print(Assembly output) {
                out.println(output.toString());
            }
        };
    }

    public static ProfilerPrinter prettyPrinter() {
        return new ProfilerPrinter() {
            public void print(Assembly output) {
                out.println(output.toPrettyString());
            }
        };
    }

    public static class ServerOutputException extends RuntimeException {
        public ServerOutputException(String msg) {
            super(msg);
        }
    }

    public static class FileOutputException extends RuntimeException {
        public FileOutputException(String msg) {
            super(msg);
        }
    }
}
