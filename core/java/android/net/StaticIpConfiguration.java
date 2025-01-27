/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.net;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SystemApi;
import android.annotation.TestApi;
import android.annotation.UnsupportedAppUsage;
import android.net.shared.InetAddressUtils;
import android.os.Parcel;
import android.os.Parcelable;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Class that describes static IP configuration.
 *
 * This class is different from LinkProperties because it represents
 * configuration intent. The general contract is that if we can represent
 * a configuration here, then we should be able to configure it on a network.
 * The intent is that it closely match the UI we have for configuring networks.
 *
 * In contrast, LinkProperties represents current state. It is much more
 * expressive. For example, it supports multiple IP addresses, multiple routes,
 * stacked interfaces, and so on. Because LinkProperties is so expressive,
 * using it to represent configuration intent as well as current state causes
 * problems. For example, we could unknowingly save a configuration that we are
 * not in fact capable of applying, or we could save a configuration that the
 * UI cannot display, which has the potential for malicious code to hide
 * hostile or unexpected configuration from the user: see, for example,
 * http://b/12663469 and http://b/16893413 .
 *
 * @hide
 */
@SystemApi
@TestApi
public final class StaticIpConfiguration implements Parcelable {
    /** @hide */
    @UnsupportedAppUsage
    @Nullable
    public LinkAddress ipAddress;
    /** @hide */
    @UnsupportedAppUsage
    @Nullable
    public InetAddress gateway;
    /** @hide */
    @UnsupportedAppUsage
    @NonNull
    public final ArrayList<InetAddress> dnsServers;
    /** @hide */
    @UnsupportedAppUsage
    @Nullable
    public String domains;

    public StaticIpConfiguration() {
        dnsServers = new ArrayList<InetAddress>();
    }

    public StaticIpConfiguration(@Nullable StaticIpConfiguration source) {
        this();
        if (source != null) {
            // All of these except dnsServers are immutable, so no need to make copies.
            ipAddress = source.ipAddress;
            gateway = source.gateway;
            dnsServers.addAll(source.dnsServers);
            domains = source.domains;
        }
    }

    public void clear() {
        ipAddress = null;
        gateway = null;
        dnsServers.clear();
        domains = null;
    }

    public @Nullable LinkAddress getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(@Nullable LinkAddress ipAddress) {
        this.ipAddress = ipAddress;
    }

    public @Nullable InetAddress getGateway() {
        return gateway;
    }

    public void setGateway(@Nullable InetAddress gateway) {
        this.gateway = gateway;
    }

    public @NonNull List<InetAddress> getDnsServers() {
        return dnsServers;
    }

    public @Nullable String getDomains() {
        return domains;
    }

    public void setDomains(@Nullable String newDomains) {
        domains = newDomains;
    }

    /**
     * Add a DNS server to this configuration.
     */
    public void addDnsServer(@NonNull InetAddress server) {
        dnsServers.add(server);
    }

    /**
     * Returns the network routes specified by this object. Will typically include a
     * directly-connected route for the IP address's local subnet and a default route. If the
     * default gateway is not covered by the directly-connected route, it will also contain a host
     * route to the gateway as well. This configuration is arguably invalid, but it used to work
     * in K and earlier, and other OSes appear to accept it.
     */
    public @NonNull List<RouteInfo> getRoutes(String iface) {
        List<RouteInfo> routes = new ArrayList<RouteInfo>(3);
        if (ipAddress != null) {
            RouteInfo connectedRoute = new RouteInfo(ipAddress, null, iface);
            routes.add(connectedRoute);
            if (gateway != null && !connectedRoute.matches(gateway)) {
                routes.add(RouteInfo.makeHostRoute(gateway, iface));
            }
        }
        if (gateway != null) {
            routes.add(new RouteInfo((IpPrefix) null, gateway, iface));
        }
        return routes;
    }

    /**
     * Returns a LinkProperties object expressing the data in this object. Note that the information
     * contained in the LinkProperties will not be a complete picture of the link's configuration,
     * because any configuration information that is obtained dynamically by the network (e.g.,
     * IPv6 configuration) will not be included.
     * @hide
     */
    public @NonNull LinkProperties toLinkProperties(String iface) {
        LinkProperties lp = new LinkProperties();
        lp.setInterfaceName(iface);
        if (ipAddress != null) {
            lp.addLinkAddress(ipAddress);
        }
        for (RouteInfo route : getRoutes(iface)) {
            lp.addRoute(route);
        }
        for (InetAddress dns : dnsServers) {
            lp.addDnsServer(dns);
        }
        lp.setDomains(domains);
        return lp;
    }

    @Override
    public String toString() {
        StringBuffer str = new StringBuffer();

        str.append("IP address ");
        if (ipAddress != null ) str.append(ipAddress).append(" ");

        str.append("Gateway ");
        if (gateway != null) str.append(gateway.getHostAddress()).append(" ");

        str.append(" DNS servers: [");
        for (InetAddress dnsServer : dnsServers) {
            str.append(" ").append(dnsServer.getHostAddress());
        }

        str.append(" ] Domains ");
        if (domains != null) str.append(domains);
        return str.toString();
    }

    @Override
    public int hashCode() {
        int result = 13;
        result = 47 * result + (ipAddress == null ? 0 : ipAddress.hashCode());
        result = 47 * result + (gateway == null ? 0 : gateway.hashCode());
        result = 47 * result + (domains == null ? 0 : domains.hashCode());
        result = 47 * result + dnsServers.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;

        if (!(obj instanceof StaticIpConfiguration)) return false;

        StaticIpConfiguration other = (StaticIpConfiguration) obj;

        return other != null &&
                Objects.equals(ipAddress, other.ipAddress) &&
                Objects.equals(gateway, other.gateway) &&
                dnsServers.equals(other.dnsServers) &&
                Objects.equals(domains, other.domains);
    }

    /** Implement the Parcelable interface */
    public static final Creator<StaticIpConfiguration> CREATOR =
        new Creator<StaticIpConfiguration>() {
            public StaticIpConfiguration createFromParcel(Parcel in) {
                return readFromParcel(in);
            }

            public StaticIpConfiguration[] newArray(int size) {
                return new StaticIpConfiguration[size];
            }
        };

    /** Implement the Parcelable interface */
    @Override
    public int describeContents() {
        return 0;
    }

    /** Implement the Parcelable interface */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(ipAddress, flags);
        InetAddressUtils.parcelInetAddress(dest, gateway, flags);
        dest.writeInt(dnsServers.size());
        for (InetAddress dnsServer : dnsServers) {
            InetAddressUtils.parcelInetAddress(dest, dnsServer, flags);
        }
        dest.writeString(domains);
    }

    /** @hide */
    public static StaticIpConfiguration readFromParcel(Parcel in) {
        final StaticIpConfiguration s = new StaticIpConfiguration();
        s.ipAddress = in.readParcelable(null);
        s.gateway = InetAddressUtils.unparcelInetAddress(in);
        s.dnsServers.clear();
        int size = in.readInt();
        for (int i = 0; i < size; i++) {
            s.dnsServers.add(InetAddressUtils.unparcelInetAddress(in));
        }
        s.domains = in.readString();
        return s;
    }
}
