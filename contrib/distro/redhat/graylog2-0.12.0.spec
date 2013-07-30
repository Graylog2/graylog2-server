Name:		graylog2
Version:	0.9.6
Release:	1%{?dist}
Summary:	Graylog2 is an open source log management solution that stores your logs in ElasticSearch.
Group:		System Environment/Daemons 
License:	GPLv2
URL:		http://www.graylog2.org/
Source0:	https://github.com/downloads/Graylog2/graylog2-server/%{name}-server-%{version}.tar.gz
Source1:    graylog2.drl
Source2:    graylog2.conf
Source3:    graylog2.init
BuildRoot:	%(mktemp -ud %{_tmppath}/%{name}-server-%{version}-%{release}-XXXXXX)

Requires:	jre >= 1.6.0

%description
Graylog2 is an open source syslog implementation that stores your logs in ElasticSearch. It consists of a server written in Java that accepts your syslog messages via TCP or UDP and stores it in the database. The second part is a Ruby on Rails web interface that allows you to view the log messages.


%prep
%setup -q -n %{name}-server-%{version}


%build


%install
rm -rf $RPM_BUILD_ROOT
# Directories
%{__install} -p -d -m 0755 %{buildroot}%{_sysconfdir}/%{name}
%{__install} -p -d -m 0755 %{buildroot}%{_sysconfdir}/%{name}/rules
%{__install} -p -d -m 0755 %{buildroot}%{_datadir}/%{name}
%{__install} -p -d -m 0755 %{buildroot}%{_localstatedir}/log/%{name}
mkdir -p %{buildroot}%{_datadir}/%{name}/plugin/{inputs,filters,outputs,alarm_callbacks,transports,initializers};

# Files
%{__install} -p -D -m 0755 %{SOURCE3} %{buildroot}%{_initrddir}/%{name}
%{__install} -p -D -m 0644 %{SOURCE1} %{buildroot}%{_sysconfdir}/%{name}/rules/%{name}.drl
%{__install} -p -D -m 0644 %{SOURCE2} %{buildroot}%{_sysconfdir}/%{name}/%{name}.conf

%{__install} -p -D -m 0644 %{name}-server.jar %{buildroot}%{_datadir}/%{name}/%{name}-server.jar


%clean
rm -rf $RPM_BUILD_ROOT


%post 
/sbin/chkconfig --add %{name}


%preun
if [[ $1 -ge 1 ]]
then
    /sbin/service %{name} stop > /dev/null 2>&1
    /sbin/chkconfig --del %{name}
fi


%files
%defattr(-,root,root,-)
%dir %{_sysconfdir}/%{name}
%dir %{_sysconfdir}/%{name}/rules
%config(noreplace) %{_sysconfdir}/%{name}/*.conf
%config(noreplace) %{_sysconfdir}/%{name}/rules/*.drl

%{_initrddir}/%{name}
%dir %{_datadir}/%{name}
%{_datadir}/%{name}/%{name}-server.jar
%dir %{_datadir}/%{name}/plugin/*

%dir %{_localstatedir}/log/%{name}


%changelog
* Mon Jul 29 2012 Timothy Forbes <leprasmurf@gmail.com> - 0.12.0
- Update to 0.12.0
- Add graylog2-elasticsearch.yml as a configuration
- Added empty plugin directories to make server stop complaining

* Mon Feb 6 2012 Daniel Aharon <daharon@sazze.com> - 0.9.6
- Update to 0.9.6
- Fix permissions for files/dirs.

* Mon May 16 2011 Daniel Aharon <daharon@sazze.com> - 0.9.5sazze1
- Modified Graylog2-server to better handle multiple rules in streams.

* Mon May 16 2011 Daniel Aharon <daharon@sazze.com> - 0.9.5p1
- Initial packaging for Fedora.
