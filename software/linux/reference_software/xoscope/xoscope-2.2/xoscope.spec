Name:		xoscope
Version:	2.2
Release:	1%{?dist}
License:	GPL-2.0
Group:		X11/Applications
URL:		http://xoscope.sourceforge.net/
Vendor:		Brent Baccala & others
Packager:	Franta Hanzlik
Summary:	Digital oscilloscope on PC
Summary(pl.UTF-8):	Cyfrowy oscyloskop na PC
Source0:	http://dl.sourceforge.net/xoscope/%{name}-%{version}.tar.gz
BuildRequires:	autoconf
BuildRequires:	automake
BuildRequires:  alsa-devel
BuildRequires:  fftw3-devel
BuildRequires:  libesd-devel
BuildRequires:  libgtkdatabox-devel
BuildRequires:  update-desktop-files
BuildRequires:	esound-devel
BuildRequires:	gtk2-devel >= 2.18
BuildRequires:	comedilib-devel
BuildRequires:  gdk-pixbuf
BuildRoot:	%{tmpdir}/%{name}-%{version}
 
%description
xoscope is a digital oscilloscope that uses a sound card, COMEDI,
and/or EsounD as the signal input.  Includes 8 signal displays,
variable time scale, math, memory, measurements, and file save/load.
 
%prep
%setup -q
 
%build
%{__aclocal}
%{__autoheader}
%{__automake}
%{__autoconf}
%configure
%{__make}
 
%install
%make_install

%suse_update_desktop_file -i xoscope
 
%clean
rm -rf $RPM_BUILD_ROOT
 
%files
%defattr(644,root,root,755)
%doc AUTHORS ChangeLog hardware NEWS README TODO
%attr(755,root,root) %{_bindir}/%{name}
%{_datadir}/pixmaps/%{name}.png
%{_datadir}/applications/%{name}.desktop
%{_mandir}/man1/%{name}.1.gz
 
%changelog
* Thu Sep 15 2016 Brent Baccala <cosine@freesoft.org>
- 2.2-1
- add AppStream support
- minor bug fixes

* Mon Jan 12 2015 Brent Baccala <cosine@freesoft.org>
- 2.1-1
- remove pl, cz translations that I don't understand

* Sun Jul 22 2012 Franta Hanzlik <franra@hanzlici.cz>
- build for Fedora distros
- patch for nonexistent asm/page.h and no. of comedi_get_cmd_generic_timed params in comedi.c
- disable CFLAGS 'GTK_DISABLE_DEPRECATED' in gtkdatabox-0.6.0.0/gtk/Makefile.am
- added .desktop file
- added notes for xoscope on Fedora distro

* Tue Jul 20 2010 UTC PLD Team <feedback@pld-linux.org>
Revision 1.9  2010/07/20 14:01:17  draenog
- up to 2.0
- remove needless pmake.patch
- add patch for TK_WIDGET_STATE undefined error
  (taken from Debian)
 
Revision 1.8  2010/01/01 22:33:08  sparky
- BR capitalization
 
Revision 1.7  2007/02/12 22:09:25  glen
- tabs in preamble
 
Revision 1.6  2007/02/12 01:06:40  baggins
- converted to UTF-8
 
Revision 1.5  2005/08/03 18:52:26  qboosh
- better errors explanation
 
Revision 1.4  2005/08/03 18:38:12  qboosh
- pl fixes, cosmetics
 
Revision 1.3  2005/08/01 15:45:03  witekfl
- added pmake.patch
- rel 0.2
 
Revision 1.2  2005/07/31 21:50:34  glen
- fix for multiple make jobs failing build
- doesn't build on amd64
 
Revision 1.1  2005/07/31 21:39:43  havner
- by Jacek 'jackass' Brzozowski <metallowiec@op.pl>
- BR fixes
