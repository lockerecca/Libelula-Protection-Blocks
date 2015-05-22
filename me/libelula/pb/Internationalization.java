/*
 *     This file is part of Libelula Protection Blocks plugin.
 *
 *  Libelula Logger is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Libelula Logger is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Libelula Logger.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package me.libelula.pb;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * Class Internationalization of the plugin.
 *
 * @author Diego Lucio D'Onofrio <ddonofrio@member.fsf.org>
 * @version 1.0
 */
public class Internationalization {

    private FileConfiguration language;

    public Internationalization(FileConfiguration language) {
        this.language = language;
    }
    
    public void setLang(FileConfiguration language) {
        this.language = language;
    }

    public String getText(String label) {
        if (language == null) {
            return label;
        }
        String text = language.getString(label);
        if (text != null) {
            return text;
        } else {
            return label;
        }
    }
}
