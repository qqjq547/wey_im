package framework.telegram.business.bridge.bean

import android.os.Parcel
import android.os.Parcelable

data class SelectedUsersModel(val uid: Long, val icon: String, val name: String) : Parcelable {

    constructor(parcel: Parcel) : this(parcel.readLong(), parcel.readString()?:"", parcel.readString()?:"")

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(uid)
        parcel.writeString(icon)
        parcel.writeString(name)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<SelectedUsersModel> {
        override fun createFromParcel(parcel: Parcel): SelectedUsersModel {
            return SelectedUsersModel(parcel)
        }

        override fun newArray(size: Int): Array<SelectedUsersModel?> {
            return arrayOfNulls(size)
        }
    }
}