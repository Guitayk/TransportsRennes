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
 * 
 * Contributors:
 *     ybonnel - initial API and implementation
 */
package fr.ybo.transportsbordeaux.modele;

import java.io.Serializable;

import fr.ybo.transportsbordeaux.util.ObjetWithDistance;

/**
 * @author ybonnel
 */
@SuppressWarnings("serial")
public class Parking extends ObjetWithDistance implements Serializable {

	public String id;
	public String name;
	public double latitude;
	public double longitude;
	public Integer carParkAvailable;
	public Integer carParkCapacity;

	/**
	 * @return the latitude
	 */
	@Override
	public double getLatitude() {
		return latitude;
	}

	/**
	 * @return the longitude
	 */
	@Override
	public double getLongitude() {
		return longitude;
	}

	@Override
	public String toString() {
		return "ParkRelai [id=" + id + ", name=" + name + ", latitude=" + latitude + ", longitude=" + longitude
				+ ", carParkAvailable=" + carParkAvailable + ", carParkCapacity=" + carParkCapacity + "]";
	}

}