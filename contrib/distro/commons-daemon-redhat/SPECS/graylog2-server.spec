%define _prefix /opt
%define _docdir /usr/share/doc

Name:		graylog2-server
Version:	0.10.0.rc.1
Release:	2%{?dist}
Summary:	Graylog2 Server

Group:		System Environment/Daemons
License:	GPLv3
URL:		http://graylog2.org/
Source0:	https://github.com/downloads/Graylog2/graylog2-server/graylog2-server-0.10.0-rc.1.tar.gz
Source1:    graylog2.drl
Source2:    graylog2.conf
Source3:    graylog2.init
Source4:    graylog2-elasticsearch.yml
Source5:    graylog2-log4j.xml
BuildRoot:	%(mktemp -ud %{_tmppath}/%{name}-server-%{version}-%{release}-XXXXXX)

Requires:	java-1.6.0-openjdk
Requires:   apache-commons-daemon-jsvc

%description
Graylog2 is an open source syslog implementation that stores your logs in ElasticSearch. It consists of a server written in Java that accepts your syslog messages via TCP or UDP and stores it in the database. The second part is a Ruby on Rails web interface that allows you to view the log messages.

%prep
%setup -q -n graylog2-server-0.10.0-rc.1


%build
true

%install
rm -rf $RPM_BUILD_ROOT

# Base
%{__mkdir} -p %{buildroot}%{_prefix}/%{name}
%{__install} -p -D -m 0644 build_date %{buildroot}%{_prefix}/%{name}
%{__install} -p -D -m 0644 COPYING %{buildroot}%{_prefix}/%{name}
%{__install} -p -D -m 0644 graylog2.conf.example %{buildroot}%{_prefix}/%{name}
%{__install} -p -D -m 0644 graylog2-server.jar %{buildroot}%{_prefix}/%{name}
%{__install} -p -D -m 0644 README.markdown %{buildroot}%{_prefix}/%{name}
%{__mkdir} -p %{buildroot}%{_prefix}/%{name}/bin
%{__install} -p -D -m 754 bin/* %{buildroot}%{_prefix}/%{name}/bin
%{__mkdir} -p %{buildroot}%{_prefix}/%{name}/plugin
%{__mkdir} -p %{buildroot}%{_prefix}/%{name}/plugin/alarm_callbacks
%{__mkdir} -p %{buildroot}%{_prefix}/%{name}/plugin/filters
%{__mkdir} -p %{buildroot}%{_prefix}/%{name}/plugin/initializers
%{__mkdir} -p %{buildroot}%{_prefix}/%{name}/plugin/inputs
%{__mkdir} -p %{buildroot}%{_prefix}/%{name}/plugin/outputs
%{__mkdir} -p %{buildroot}%{_prefix}/%{name}/plugin/transports

# config
%{__mkdir} -p %{buildroot}%{_sysconfdir}/%{name}/rules
%{__install} -p -D -m 0644 %{SOURCE2} %{buildroot}%{_sysconfdir}/%{name}/graylog2.conf
%{__install} -p -D -m 0644 %{SOURCE4} %{buildroot}%{_sysconfdir}/%{name}/elasticsearch.yml
%{__install} -p -D -m 0644 %{SOURCE5} %{buildroot}%{_sysconfdir}/%{name}/log4j.xml
%{__install} -p -D -m 0644 %{SOURCE1} %{buildroot}%{_sysconfdir}/%{name}/rules/graylog2.drl

# logs
%{__mkdir} -p %{buildroot}%{_localstatedir}/log/%{name}

# sysconfig and init
%{__mkdir} -p %{buildroot}%{_sysconfdir}/rc.d/init.d
%{__install} -m 755 %{SOURCE3} %{buildroot}%{_sysconfdir}/rc.d/init.d/%{name}

%{__mkdir} -p %{buildroot}%{_localstatedir}/run/%{name}
%{__mkdir} -p %{buildroot}%{_localstatedir}/lock/subsys/%{name}

%pre
# create graylog2 group
if ! getent group graylog2 >/dev/null; then
        /usr/sbin/groupadd -r graylog2
fi

# create graylog2 user
if ! getent passwd graylog2 >/dev/null; then
        /usr/sbin/useradd -r -g graylog2 -d %{_prefix}/%{name} \
            -s /sbin/nologin -c "log aggregator" graylog2
fi

%post 
/sbin/chkconfig --add %{name}


%preun
if [[ $1 -ge 1 ]]
then
    /sbin/service %{name} stop > /dev/null 2>&1
    /sbin/chkconfig --del %{name}
fi


%clean
rm -rf $RPM_BUILD_ROOT


%files
%defattr(-,root,root,-)
%{_sysconfdir}/rc.d/init.d/%{name}
%dir %{_sysconfdir}/%{name}
%dir %{_sysconfdir}/%{name}/rules
%config(noreplace) %{_sysconfdir}/%{name}/*.conf
%config(noreplace) %{_sysconfdir}/%{name}/*.yml
%config(noreplace) %{_sysconfdir}/%{name}/*.xml
%config(noreplace) %{_sysconfdir}/%{name}/rules/*.drl

%defattr(-,graylog2,graylog2,-)
%{_prefix}/%{name}
%{_prefix}/%{name}/bin
%{_prefix}/%{name}/plugin
%{_prefix}/%{name}/plugin/alarm_callbacks
%{_prefix}/%{name}/plugin/filters
%{_prefix}/%{name}/plugin/initializers
%{_prefix}/%{name}/plugin/inputs
%{_prefix}/%{name}/plugin/outputs
%{_prefix}/%{name}/plugin/transports
%dir %{_localstatedir}/run/%{name}
%dir %{_localstatedir}/log/%{name}


%changelog
* Wed Dec 26 2012 kbrockhoff@codekaizen.org 0.10.0-rc.1
- Update to latest graylog2 release

* Wed Oct 24 2012 kbrockhoff@codekaizen.org 0.9.6p1-2
- Initial definition

