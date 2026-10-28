package com.equadis.portal.modules.auth.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import com.equadis.portal.modules.auth.domain.UserAccount;
import com.equadis.portal.modules.auth.domain.UserAccountRepository;

/** Spring Data adapter for the {@link UserAccountRepository} port. */
interface SpringDataUserAccountRepository extends JpaRepository<UserAccount, Long>, UserAccountRepository {

}
