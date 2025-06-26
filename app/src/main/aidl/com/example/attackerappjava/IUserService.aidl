// IUserService.aidl
package com.example.attackerappjava;

// Declare any non-default types here with import statements

interface IUserService {
  void gatherPermission(String permission);
  boolean triggerOverlay();
  void writeToFile(String fileName,String content);
}