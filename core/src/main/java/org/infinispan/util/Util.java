/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2000 - 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.infinispan.util;

import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * General utility methods used throughout the JBC code base.
 *
 * @author <a href="brian.stansberry@jboss.com">Brian Stansberry</a>
 * @author Galder Zamarreño
 * @since 4.0
 */
public final class Util {

   private static final boolean isArraysDebug = Boolean.getBoolean("infinispan.arrays.debug");

   /**
    * Loads the specified class using this class's classloader, or, if it is <code>null</code> (i.e. this class was
    * loaded by the bootstrap classloader), the system classloader. <p/> If loadtime instrumentation via
    * GenerateInstrumentedClassLoader is used, this class may be loaded by the bootstrap classloader. </p>
    *
    * @param classname name of the class to load
    * @return the class
    * @throws ClassNotFoundException
    */
   public static Class loadClass(String classname) throws ClassNotFoundException {
      ClassLoader cl = Thread.currentThread().getContextClassLoader();
      if (cl == null)
         cl = ClassLoader.getSystemClassLoader();
      return cl.loadClass(classname);
   }

   private static Method getFactoryMethod(Class c) {
      for (Method m : c.getMethods()) {
         if (m.getName().equals("getInstance") && m.getParameterTypes().length == 0 && Modifier.isStatic(m.getModifiers()))
            return m;
      }
      return null;
   }

   @SuppressWarnings("unchecked")
   public static <T> T getInstance(Class<T> clazz) throws IllegalAccessException, InstantiationException {
      // first look for a getInstance() constructor
      T instance = null;
      try {
         Method factoryMethod = getFactoryMethod(clazz);
         if (factoryMethod != null) instance = (T) factoryMethod.invoke(null);
      }
      catch (Exception e) {
         // no factory method or factory method failed.  Try a constructor.
         instance = null;
      }
      if (instance == null) instance = clazz.newInstance();
      return instance;
   }

   @SuppressWarnings("unchecked")
   public static Object getInstance(String classname) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
      if (classname == null) throw new IllegalArgumentException("Cannot load null class!");
      Class clazz = loadClass(classname);
      return getInstance(clazz);
   }

   /**
    * Prevent instantiation
    */
   private Util() {
   }

   /**
    * Null-safe equality test.
    *
    * @param a first object to compare
    * @param b second object to compare
    * @return true if the objects are equals or both null, false otherwise.
    */
   public static boolean safeEquals(Object a, Object b) {
      return (a == b) || (a != null && a.equals(b));
   }

   public static InputStream loadResourceAsStream(String resource) {
      ClassLoader cl = Thread.currentThread().getContextClassLoader();
      InputStream s = cl.getResourceAsStream(resource);
      if (s == null) {
         cl = Util.class.getClassLoader();
         s = cl.getResourceAsStream(resource);
      }
      return s;
   }

   /**
    * Static inner class that holds 3 maps - for data added, removed and modified.
    */
   public static class MapModifications {
      public final Map<Object, Object> addedEntries = new HashMap<Object, Object>();
      public final Map<Object, Object> removedEntries = new HashMap<Object, Object>();
      public final Map<Object, Object> modifiedEntries = new HashMap<Object, Object>();


      @Override
      public boolean equals(Object o) {
         if (this == o) return true;
         if (o == null || getClass() != o.getClass()) return false;

         MapModifications that = (MapModifications) o;

         if (addedEntries != null ? !addedEntries.equals(that.addedEntries) : that.addedEntries != null) return false;
         if (modifiedEntries != null ? !modifiedEntries.equals(that.modifiedEntries) : that.modifiedEntries != null)
            return false;
         if (removedEntries != null ? !removedEntries.equals(that.removedEntries) : that.removedEntries != null)
            return false;

         return true;
      }

      @Override
      public int hashCode() {
         int result;
         result = (addedEntries != null ? addedEntries.hashCode() : 0);
         result = 31 * result + (removedEntries != null ? removedEntries.hashCode() : 0);
         result = 31 * result + (modifiedEntries != null ? modifiedEntries.hashCode() : 0);
         return result;
      }

      @Override
      public String toString() {
         return "Added Entries " + addedEntries + " Removed Entries " + removedEntries + " Modified Entries " + modifiedEntries;
      }
   }

   public static String prettyPrintTime(long time, TimeUnit unit) {
      return prettyPrintTime(unit.toMillis(time));
   }

   /**
    * Prints a time for display
    *
    * @param millis time in millis
    * @return the time, represented as millis, seconds, minutes or hours as appropriate, with suffix
    */
   public static String prettyPrintTime(long millis) {
      if (millis < 1000) return millis + " milliseconds";
      NumberFormat nf = NumberFormat.getNumberInstance();
      nf.setMaximumFractionDigits(2);
      double toPrint = ((double) millis) / 1000;
      if (toPrint < 300) {
         return nf.format(toPrint) + " seconds";
      }

      toPrint = toPrint / 60;

      if (toPrint < 120) {
         return nf.format(toPrint) + " minutes";
      }

      toPrint = toPrint / 60;

      return nf.format(toPrint) + " hours";
   }

   public static void closeStream(InputStream i) {
      if (i == null) return;
      try {
         i.close();
      } catch (Exception e) {

      }
   }

   public static void closeInput(ObjectInput i) {
      if (i == null) return;
      try {
         i.close();
      } catch (Exception e) {

      }
   }

   public static void flushAndCloseStream(OutputStream o) {
      if (o == null) return;
      try {
         o.flush();
      } catch (Exception e) {

      }

      try {
         o.close();
      } catch (Exception e) {

      }
   }

   public static void flushAndCloseOutput(ObjectOutput o) {
      if (o == null) return;
      try {
         o.flush();
      } catch (Exception e) {

      }

      try {
         o.close();
      } catch (Exception e) {

      }
   }

   public static String formatString(Object message, Object... params) {
      if (params.length == 0) return message == null ? "null" : message.toString();

      StringBuilder value = new StringBuilder(String.valueOf(message));
      for (int i = 0; i < params.length; i++) {
         String placeholder = "{" + i + "}";
         int phIndex;
         if ((phIndex = value.indexOf(placeholder)) > -1) {
            value = value.replace(phIndex, phIndex + placeholder.length(), String.valueOf(params[i]));
         }
      }
      return value.toString();
   }

   public static String printArray(byte[] array, boolean withHash) {
      return printArray(array, withHash, isArraysDebug);
   }

   public static String printArray(byte[] array, boolean withHash, boolean isDebug) {
      if (array == null) return "null";
      StringBuilder sb = new StringBuilder();
      sb.append("ByteArray{size=").append(array.length);
      if (withHash)
         sb.append(", hashCode=").append(Integer.toHexString(array.hashCode()));

      sb.append(", array=0x");
      if (isDebug) {
         // Convert the entire byte array
         sb.append(toHexString(array));
      } else {
         // Pick the first 8 characters and convert that part
         sb.append(toHexString(array, 8));
         sb.append("..");
      }
      sb.append("}");

      return sb.toString();
   }

   public static String toHexString(byte input[]) {
      return toHexString(input, input.length);
   }

   public static String toHexString(byte input[], int limit) {
      int i = 0;
      if (input == null || input.length <= 0)
         return null;

      char lookup[] = {'0', '1', '2', '3', '4', '5', '6', '7',
                       '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

      char[] result = new char[(input.length < limit ? input.length : limit) * 2];

      while (i < limit && i < input.length) {
         result[2*i] = lookup[(input[i] >> 4) & 0x0F];
         result[2*i+1] = lookup[(input[i] & 0x0F)];
         i++;
      }
      return String.valueOf(result);
   }

}
