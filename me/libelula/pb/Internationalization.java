package me.libelula.pb;

import org.bukkit.configuration.file.FileConfiguration;

public class Internationalization
{
  private FileConfiguration language;
  
  public Internationalization(FileConfiguration language)
  {
    this.language = language;
  }
  
  public void setLang(FileConfiguration language)
  {
    this.language = language;
  }
  
  public String getText(String label)
  {
    if (this.language == null) {
      return label;
    }
    String text = this.language.getString(label);
    if (text != null) {
      return text;
    }
    return label;
  }
}
