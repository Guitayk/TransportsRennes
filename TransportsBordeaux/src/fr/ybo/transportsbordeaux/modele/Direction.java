/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package fr.ybo.transportsbordeaux.modele;

import fr.ybo.moteurcsv.adapter.AdapterInteger;
import fr.ybo.moteurcsv.annotation.BaliseCsv;
import fr.ybo.moteurcsv.annotation.FichierCsv;
import fr.ybo.transportsbordeaux.TransportsBordeauxApplication;
import fr.ybo.transportsbordeaux.database.annotation.Colonne;
import fr.ybo.transportsbordeaux.database.annotation.PrimaryKey;
import fr.ybo.transportsbordeaux.database.annotation.Table;

@FichierCsv("directions.txt")
@Table
public class Direction {
	@BaliseCsv(value = "id", adapter = AdapterInteger.class)
	@Colonne(type = Colonne.TypeColonne.INTEGER)
	@PrimaryKey
	public Integer id;
	@BaliseCsv("direction")
	@Colonne
	public String direction;


	private static Direction directionSelect = null;

	public static String getDirectionById(int id) {
		if (directionSelect == null) {
			directionSelect = new Direction();
		}
		directionSelect.id = id;
		return TransportsBordeauxApplication.getDataBaseHelper().selectSingle(directionSelect).direction;
	}
}